package com.envelopes.apps.labelprinter.paper;

import java.awt.print.Paper;

/**
 * Created by Manu on 7/20/2016.
 */
public class Label3x2 extends Paper {
    protected static final double width = 3.5;
    protected static final double height = 2.0;
    protected static final double leftMargin = 0.5;
    protected static final double rightMargin = 0.0;
    protected static final double topMargin = 0.0;
    protected static final double bottomMargin = 0.0;
    protected static final double scale = 72.0;

    public Label3x2() {
        super();
        this.setSize(width * scale, height * scale);
        this.setImageableArea(leftMargin * scale, topMargin * scale, (width - leftMargin - rightMargin) * scale, (height - topMargin - bottomMargin) * scale);
    }
}
