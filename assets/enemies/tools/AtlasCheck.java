import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import java.io.File;
public class AtlasCheck {
  public static void main(String[] a) {
    FileHandle f = new FileHandle(new File(a[0]));
    TextureAtlasData d = new TextureAtlasData(f, f.parent(), false);
    for (TextureAtlasData.Page p : d.getPages()) System.out.println("page " + p.textureFile.name() + " " + p.width + "x" + p.height + " exists=" + p.textureFile.exists());
    for (TextureAtlasData.Region r : d.getRegions()) System.out.println(r.name + " index=" + r.index + " xy=" + r.left + "," + r.top + " size=" + r.width + "x" + r.height);
  }
}
