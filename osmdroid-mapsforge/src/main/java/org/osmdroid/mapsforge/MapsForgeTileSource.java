package org.osmdroid.mapsforge;

import android.app.job.JobParameters;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;

import java.io.File;

/**
 * http://www.salidasoftware.com/how-to-render-mapsforge-tiles-in-osmdroid/
 *
 * @author Salida Software
 *         Adapted from code found here : http://www.sieswerda.net/2012/08/15/upping-the-developer-friendliness/
 */
public class MapsForgeTileSource extends BitmapTileSourceBase {

    // Reasonable defaults ..
    public static final int MIN_ZOOM = 8;
    public static final int MAX_ZOOM = 20;
    public static final int TILE_SIZE_PIXELS = 256;

    private DatabaseRenderer renderer;

    private MultiMapDataStore mapDatabase;

    /**
     * The reason this constructor is protected is because all parameters,
     * except file should be determined from the archive file. Therefore a
     * factory method is necessary.
     *
     * @param minZoom
     * @param maxZoom
     * @param tileSizePixels
     * @param file
     */
    protected MapsForgeTileSource(int minZoom, int maxZoom, int tileSizePixels, File[] file) {
        super("MapsForgeTiles", minZoom, maxZoom, tileSizePixels, ".png");

        mapDatabase = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_FIRST);
        for (int i=0; i < file.length; i++)
            mapDatabase.addMapDataStore(new MapFile(file[i]),false,false);


        renderer = new DatabaseRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, new InMemoryTileCache(2));

        minZoom = renderer.getStartZoomLevel();
        maxZoom = renderer.getZoomLevelMax();

        Log.d("MAPSFORGE", "min=" + minZoom + " max=" + maxZoom + " tilesize=" + tileSizePixels);

        //  For this to work I had to edit org.mapsforge.map.rendertheme.InternalRenderTheme.getRenderThemeAsStream()  to:
        //  return this.getClass().getResourceAsStream(this.absolutePath + this.file);
      //  jobTheme = InternalRenderTheme.OSMARENDER;

    }

    /**
     * Creates a new MapsForgeTileSource from file.
     * <p/>
     * Parameters minZoom and maxZoom are obtained from the
     * database. If they cannot be obtained from the DB, the default values as
     * defined by this class are used.
     *
     * @param file
     * @return the tile source
     */
    public static MapsForgeTileSource createFromFile(File[] file) {
        //TODO - set these based on .map file info
        int minZoomLevel = MIN_ZOOM;
        int maxZoomLevel = MAX_ZOOM;
        int tileSizePixels = TILE_SIZE_PIXELS;

        return new MapsForgeTileSource(minZoomLevel, maxZoomLevel, tileSizePixels, file);
    }

    //The synchronized here is VERY important.  If missing, the mapDatabase read gets corrupted by multiple threads reading the file at once.
    public synchronized Drawable renderTile(MapTile pTile) {

        Tile tile = new Tile( pTile.getX(),  pTile.getY(), (byte) pTile.getZoomLevel(), 256);

        //Create a bitmap to draw on
        Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE_PIXELS, TILE_SIZE_PIXELS, Bitmap.Config.RGB_565);

        //You could try something like this to load a custom theme
        //try{
        //	jobTheme = new ExternalRenderTheme(themeFile);
        //}
        //catch(Exception e){
        //	jobTheme = InternalRenderTheme.OSMARENDER;
        //}

        try {
            //Draw the tile
            // MapDataStore mapFile, RenderThemeFuture renderThemeFuture, DisplayModel displayModel,
            //       float textScale, boolean isTransparent, boolean labelsOnly

            RendererJob mapGeneratorJob = new RendererJob(tile, mapDatabase,new RenderThemeFuture(AndroidGraphicFactory.INSTANCE,
                    InternalRenderTheme.OSMARENDER, new DisplayModel()), new DisplayModel(),14f,false,false);

            renderer.executeJob(mapGeneratorJob);
        } catch (Exception ex) {
            //Make the bad tile easy to spot
            bitmap.eraseColor(Color.YELLOW);
        }

        Drawable d = new BitmapDrawable(bitmap);
        return d;
    }

}