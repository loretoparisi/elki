package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DisjointCrossValidationHoldout provides a set of partitions of a database to
 * perform cross-validation.
 * The test sets are guaranteed to be disjoint.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DisjointCrossValidationHoldout<O extends DatabaseObject> extends RandomizedHoldout<O>
{
    /**
     * Parameter n for the number of folds.
     */
    public static final String N_P = "nfold";
    
    /**
     * Default number of folds.
     */
    public static final int N_DEFAULT = 10;
    
    /**
     * Description of the parameter n.
     */
    public static final String N_D = "<int>number of folds for cross-validation";
    
    /**
     * Holds the number of folds.
     */
    protected int nfold = N_DEFAULT;
    
    /**
     * Provides a holdout for n-fold cross-validation.
     * Additionally to the parameter seed, the parameter n is set.
     */
    public DisjointCrossValidationHoldout()
    {
        super();
        parameterToDescription.put(N_P+OptionHandler.EXPECTS_VALUE,N_D);
        optionHandler = new OptionHandler(parameterToDescription,DisjointCrossValidationHoldout.class.getName());
    }

    /**
     * Provides a set of n partitions of a database to
     * perform n-fold cross-validation.
     * 
     * @see de.lmu.ifi.dbs.evaluation.holdout.Holdout#partition(de.lmu.ifi.dbs.database.Database)
     */
    public TrainingAndTestSet<O>[] partition(Database<O> database)
    {
        this.database = database;
        setClassLabels(database);
        TrainingAndTestSet<O>[] partitions = new TrainingAndTestSet[nfold];
        List<Integer> ids = database.getIDs();
        List<Integer>[] parts = new List[nfold];
        for(int i = 0; i < nfold; i++)
        {
            parts[i] = new ArrayList<Integer>();
        }
        for(Integer id : ids)
        {
            parts[random.nextInt(nfold)].add(id);
            
        }
        for(int i = 0; i < nfold; i++)
        {
            Map<Integer,List<Integer>> partition = new HashMap<Integer,List<Integer>>();
            List<Integer> training = new ArrayList<Integer>();
            for(int j = 0; j < nfold; j++)
            {
                if(j!=i)
                {
                    training.addAll(parts[j]);
                }
            }
            partition.put(0,training);
            partition.put(1,parts[i]);
            try
            {
                Map<Integer,Database<O>> part = database.partition(partition);
                partitions[i] = new TrainingAndTestSet<O>(part.get(0),part.get(1),this.labels);
            }
            catch(UnableToComplyException e)
            {
                throw new RuntimeException(e);
            }
        }
        return partitions;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        return "Provides an n-fold cross-validation holdout with disjoint test sets.";
    }

    /**
     * Sets the parameter n.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(N_P))
        {
            try
            {
                int nfold = Integer.parseInt(optionHandler.getOptionValue(N_P));
                if(nfold<1)
                {
                    throw new NumberFormatException("Parameter "+N_P+" is supposed to be a positiv integer. Found: "+optionHandler.getOptionValue(N_P));
                }
                this.nfold = nfold;
            }
            catch(NumberFormatException e)
            {
                throw new IllegalArgumentException("Parameter "+N_P+" is supposed to be a positiv integer. Found: "+optionHandler.getOptionValue(N_P),e);
            }
        }
        return remainingParameters;
    }

    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        AttributeSettings attributeSettings = settings.get(0);
        attributeSettings.addSetting(N_P,Integer.toString(nfold));
        settings.add(attributeSettings);
        return settings;
    }
    
    
}
