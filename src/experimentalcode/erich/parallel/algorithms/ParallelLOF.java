package experimentalcode.erich.parallel.algorithms;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.erich.parallel.ParallelMapExecutor;
import experimentalcode.erich.parallel.mapper.DoubleMinMaxMapper;
import experimentalcode.erich.parallel.mapper.KDistanceMapper;
import experimentalcode.erich.parallel.mapper.KNNMapper;
import experimentalcode.erich.parallel.mapper.LOFMapper;
import experimentalcode.erich.parallel.mapper.LRDMapper;
import experimentalcode.erich.parallel.mapper.WriteDataStoreMapper;
import experimentalcode.erich.parallel.mapper.WriteDoubleDataStoreMapper;
import experimentalcode.erich.parallel.variables.SharedDouble;
import experimentalcode.erich.parallel.variables.SharedObject;

/**
 * Parallel implementation of Local Outlier Factor using mappers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class ParallelLOF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Parameter k
   */
  private int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k K parameter
   */
  public ParallelLOF(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelLOF.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<O> distq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnq = database.getKNNQuery(distq, k + 1);

    // Phase one: KNN and k-dist
    WritableDoubleDataStore kdists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    WritableDataStore<KNNList> knns = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, KNNList.class);
    {
      // Compute kNN
      KNNMapper<O> knnm = new KNNMapper<>(k + 1, knnq);
      SharedObject<KNNList> knnv = new SharedObject<>();
      WriteDataStoreMapper<KNNList> storek = new WriteDataStoreMapper<>(knns);
      knnm.connectKNNOutput(knnv);
      storek.connectInput(knnv);
      // Compute k-dist
      KDistanceMapper kdistm = new KDistanceMapper(k + 1);
      SharedDouble kdistv = new SharedDouble();
      WriteDoubleDataStoreMapper storem = new WriteDoubleDataStoreMapper(kdists);
      kdistm.connectKNNInput(knnv);
      kdistm.connectOutput(kdistv);
      storem.connectInput(kdistv);

      ParallelMapExecutor.run(ids, knnm, storek, kdistm, storem);
    }

    // Phase two: lrd
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    {
      LRDMapper lrdm = new LRDMapper(knns, kdists);
      SharedDouble lrdv = new SharedDouble();
      WriteDoubleDataStoreMapper storelrd = new WriteDoubleDataStoreMapper(lrds);

      lrdm.connectOutput(lrdv);
      storelrd.connectInput(lrdv);
      ParallelMapExecutor.run(ids, lrdm, storelrd);
    }
    kdists.destroy(); // No longer needed.
    kdists = null;

    // Phase three: LOF
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax minmax;
    {
      LOFMapper lofm = new LOFMapper(knns, lrds, true);
      SharedDouble lofv = new SharedDouble();
      DoubleMinMaxMapper mmm = new DoubleMinMaxMapper();
      WriteDoubleDataStoreMapper storelof = new WriteDoubleDataStoreMapper(lofs);

      lofm.connectOutput(lofv);
      mmm.connectInput(lofv);
      storelof.connectInput(lofv);
      ParallelMapExecutor.run(ids, lofm, storelof, mmm);

      minmax = mmm.getMinMax();
    }

    DoubleRelation scoreres = new MaterializedDoubleRelation("Local Outlier Factor", "lof-outlier", lofs, ids);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * K parameter
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(LOF.Parameterizer.K_ID);
      if (config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected ParallelLOF<O> makeInstance() {
      return new ParallelLOF<>(distanceFunction, k);
    }
  }
}
