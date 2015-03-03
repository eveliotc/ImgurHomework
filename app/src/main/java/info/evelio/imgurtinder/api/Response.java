package info.evelio.imgurtinder.api;

import java.util.List;

public class Response {
  private List<Image> data;

  public List<Image> getData() {
    return data;
  }

  public void setData(List<Image> data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "Response{" +
        "data=" + data +
        '}';
  }
}
