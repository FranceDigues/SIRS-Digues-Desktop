package fr.sirs.util;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import java.text.NumberFormat;
import java.util.logging.Level;
import javax.measure.Unit;
import org.apache.sis.measure.Units;
import org.geotoolkit.display.MeasureUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public final class SIRSAreaComputer {


    /**
     * HACK used as curent implementation of {@link MeasureUtilities#calculateArea(com.vividsolutions.jts.geom.Geometry, org.opengis.referencing.crs.CoordinateReferenceSystem, javax.measure.Unit) }
     * return 0 for multi-polygons (SYM-1965).
     *
     * @param geometry
     * @param geomCRS
     * @param unit
     * @return
     */
    public static final double calculateArea(final Geometry geometry, final CoordinateReferenceSystem geomCRS, final Unit unit) {

        double area =0;
        if (geometry instanceof MultiPolygon) {
                final MultiPolygon multiPolygon = ((MultiPolygon) geometry);
                final int number = multiPolygon.getNumGeometries();
                for (int i=0; i< number; i++) {
                    try {
                        area += MeasureUtilities.calculateArea((Polygon) multiPolygon.getGeometryN(i), geomCRS, unit);
                    } catch (Exception ex) {
                        SirsCore.LOGGER.log(Level.WARNING, "Exception lors de l'extraction de la géométrie du multi-polygone pour le calcule de sa surface : \n"+multiPolygon.toText()+"\n", ex);
                    }

                }
                return area;
        } else {
            area += MeasureUtilities.calculateArea(geometry, geomCRS, unit);
        }

        return area;

    }

    /**
     * Returns a label specifying the geometry area if the geometry is a Polygon
     * or MultiPolygon, or the geometry length otherwise.
     *
     * @param geometry
     */
    public static String getGeometryInfo(final Geometry geometry) {
        if (geometry != null) {
            final CoordinateReferenceSystem projection = ((SessionCore) InjectorCore.getBean(SessionCore.class)).getProjection();

            if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                final String surface = NumberFormat.getNumberInstance().format(
                        SIRSAreaComputer.calculateArea(geometry, projection, Units.SQUARE_METRE)) + " m²";
                return "Surface : " + surface;
            } else {
                final String longueur = NumberFormat.getNumberInstance().format(
                        MeasureUtilities.calculateLenght(geometry, projection, Units.METRE)) + " m";
                return "Longueur : " + longueur;
            }
        } else {
            return "";
        }
    }
}
