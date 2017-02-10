package cn.sunjian.coolmenu;

/**
 * Created by sunjian on 2017/1/19.
 */

final class CoolMenuBean {

    private int x;
    private int y;
    private int position;

    public CoolMenuBean(int x, int y, int position) {
        this.x = x;
        this.y = y;
        this.position = position;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPosition() {
        return position;
    }
}
