package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.*;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.IDPair;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Provides a database connection expecting input from standard in.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class InputStreamDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
  /**
   * Prefix for properties related to InputStreamDatabaseConnection.
   */
  public final static String PREFIX = "INPUT_STREAM_DBC_";

  /**
   * Default parser.
   */
  public final static String DEFAULT_PARSER = DoubleVectorLabelParser.class.getName();

  /**
   * Label for parameter parser.
   */
  public final static String PARSER_P = "parser";

  /**
   * Description of parameter parser.
   */
  public final static String PARSER_D = "<classname>a parser to provide a database (default: " + DEFAULT_PARSER + ")";

  /**
   * The parser.
   */
  Parser<O> parser;

  /**
   * The input to parse from.
   */
  InputStream in = System.in;

  /**
   * Provides a database connection expecting input from standard in.
   */
  @SuppressWarnings("unchecked")
  public InputStreamDatabaseConnection() {
    parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
    parser = Util.instantiate(Parser.class, DEFAULT_PARSER);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.database.connection.DatabaseConnection#getDatabase(Normalization)
   */
  @SuppressWarnings("unchecked")
  public Database<O> getDatabase(Normalization<O> normalization) {
    try {
      // parse
      ParsingResult<O> parsingResult = parser.parse(in);
      // normalize objects
      List<O> objects = normalization != null ?
                        normalization.normalize(parsingResult.getObjects()) :
                        parsingResult.getObjects();
      // transform labels
      List<Map<AssociationID, Object>> labels = transform(parsingResult.getLabels());

      // insert into database
      database.insert(objects, labels);

      // precomputed distances
      if (parser instanceof DistanceParser) {
        Map<IDPair, Distance> distanceMap = ((DistanceParsingResult<O, Distance>) parsingResult).getDistanceMap();
        DistanceFunction<O, Distance> distanceFunction = ((DistanceParser<O, Distance>) parser).getDistanceFunction();
        database.addDistancesToCache(distanceMap, (Class<DistanceFunction<O, Distance>>) distanceFunction.getClass());
      }

      return database;
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch (NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Transforms the specified labelList into a map of association id an association object
   * suitable for inserting objects into the database
   *
   * @param labelList the list to be transformes
   * @return a map of association id an association object
   */
  private List<Map<AssociationID, Object>> transform(List<String> labelList) {
    List<Map<AssociationID, Object>> result = new ArrayList<Map<AssociationID, Object>>();

    for (String label : labelList) {
      Map<AssociationID, Object> associationMap = new Hashtable<AssociationID, Object>();

      Object association;
      if (classLabel == null) {
        association = label;
      }
      else {
        try {
          association = Class.forName(classLabel).newInstance();
          ((ClassLabel) association).init(label);
        }
        catch (InstantiationException e) {
          throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        }
        catch (ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
      }
      associationMap.put(associationID, association);
      result.add(associationMap);
    }
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("", false));
    description.append('\n');
    description.append("Parsers available within this framework for database connection ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n');
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_PARSER)))
    {
      description.append("Class: ");
      description.append(pd.getEntry());
      description.append('\n');
      description.append(pd.getDescription());
      description.append('\n');
    }
    description.append('\n');
    description.append("Databases available within this framework for database connection ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n');
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_DATABASE)))
    {
      description.append("Class: ");
      description.append(pd.getEntry());
      description.append('\n');
      description.append(pd.getDescription());
      description.append('\n');
    }
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = super.setParameters(args);
    if (optionHandler.isSet(PARSER_P)) {
      parser = Util.instantiate(Parser.class, optionHandler.getOptionValue(PARSER_P));
    }
    return parser.setParameters(remainingOptions);
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    attributeSettings.addSetting(PARSER_P, parser.getClass().getName());

    result.addAll(parser.getAttributeSettings());

    return result;
  }

  /**
   * Returns the prefix for properties concerning InputStreamDatabaseConnection.
   * Extending classes requiring other properties should overwrite this method
   * to provide another prefix.
   */
  protected String propertyPrefix() {
    return PREFIX;
  }
}
