import java.util.List;

/**
 * Created by zhangshengxin on 9/7/17.
 */
public class DataNode {
    private int id;
    private List<Integer> fathers;
    private String title;
    private int titleLevel;
    private String content;

    public DataNode(String title, int titleLevel, List<Integer> fathers, int id) {
        this.title = title;
        this.titleLevel = titleLevel;
        this.fathers = fathers;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getFathers() {
        return fathers;
    }

    public void setFathers(List<Integer> fathers) {
        this.fathers = fathers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTitleLevel() {
        return titleLevel;
    }

    public void setTitleLevel(int titleLevel) {
        this.titleLevel = titleLevel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
