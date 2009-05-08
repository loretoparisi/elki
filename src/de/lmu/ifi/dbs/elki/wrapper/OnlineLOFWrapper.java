package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import experimentalcode.elke.algorithm.lof.OnlineLOF;


/**
 * Wrapper class for Online LOF algorithm.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class OnlineLOFWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {

    /**
     * Parameter to specify the number of nearest neighbors of an object to be considered for computing its LOF,
     * must be an integer greater than 0.
     * <p>Key: {@code -lof.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        OnlineLOF.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the name of the file containing tthe objects to be inserted.
     * <p>Key: {@code -onlinelof.insertions} </p>
     */
    private final FileParameter INSERTIONS_PARAM = new FileParameter(OnlineLOF.INSERTIONS_ID, FileParameter.FileType.INPUT_FILE);


    /**
     * Parameter to specify the name of the file containing the LOFs of the input file.
     * <p>Key: {@code -onlinelof.lof} </p>
     */
    private final FileParameter LOF_PARAM = new FileParameter(OnlineLOF.LOF_ID, FileParameter.FileType.INPUT_FILE);


    /**
     * Parameter to specify the name of the file containing the nearest neighbors of the input file.
     * <p>Key: {@code -onlinelof.nn} </p>
     */
    private final FileParameter NN_PARAM = new FileParameter(OnlineLOF.NN_ID, FileParameter.FileType.INPUT_FILE);


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new OnlineLOFWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameter
     * {@link #MINPTS_PARAM}, {@link #INSERTIONS_PARAM}, {@link #LOF_PARAM}, and {@link #NN_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public OnlineLOFWrapper() {
        super();
        // parameter min points
        addOption(MINPTS_PARAM);

        // parameter insertions
        addOption(INSERTIONS_PARAM);

        // parameter LOF
        addOption(LOF_PARAM);

        //parameter nn
        addOption(NN_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm OnlineLOF
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, OnlineLOF.class.getName());

        // minpts
        OptionUtil.addParameter(parameters, OnlineLOF.MINPTS_ID, Integer.toString(MINPTS_PARAM.getValue()));

        // insertions
        OptionUtil.addParameter(parameters, OnlineLOF.INSERTIONS_ID, INSERTIONS_PARAM.getValue().getPath());

        // lof
        OptionUtil.addParameter(parameters, OnlineLOF.LOF_ID, LOF_PARAM.getValue().getPath());

        // nn
        OptionUtil.addParameter(parameters, OnlineLOF.NN_ID, NN_PARAM.getValue().getPath());

        // distance function
        OptionUtil.addParameter(parameters, OnlineLOF.DISTANCE_FUNCTION_ID, EuclideanDistanceFunction.class.getName());

        // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.PAGE_SIZE_P);
//    parameters.add("8000");

        // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);


        return parameters;
    }
}
