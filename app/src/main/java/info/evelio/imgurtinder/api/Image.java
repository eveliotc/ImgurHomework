package info.evelio.imgurtinder.api;

import com.google.gson.annotations.SerializedName;

public class Image {
  private String id;
  private String link;
  @SerializedName("is_album")
  private boolean album;
  private String cover;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public boolean isAlbum() {
    return album;
  }

  public void setAlbum(boolean album) {
    this.album = album;
  }

  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
  }

  /**
   * @return
   * Link or cover if album
   */
  public String getImageUrl() {
    return album
        ? buildImgurLink(cover)
        : link;
  }

  /**
   * I know I know, assuming this for now ¯\_(ツ)_/¯
   */
  static String buildImgurLink(String cover) {
    return "http://i.imgur.com/" + cover + ".jpg";
  }

  @Override
  public String toString() {
    return "Image{" +
        "id='" + id + '\'' +
        ", link='" + link + '\'' +
        ", album=" + album +
        ", cover='" + cover + '\'' +
        '}';
  }
}
