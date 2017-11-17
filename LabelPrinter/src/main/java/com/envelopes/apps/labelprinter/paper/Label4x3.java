package com.envelopes.apps.labelprinter.paper;

import java.awt.print.Paper;

/**
 * Created by Manu on 7/20/2016.
 */
public class Label4x3 extends Paper {
    protected static final double width = 4.0;
    protected static final double height = 3.0;
    protected static final double leftMargin = 0.0;
    protected static final double rightMargin = 0.0;
    protected static final double topMargin = 0.0;
    protected static final double bottomMargin = 0.0;
    protected static final double scale = 72.0;

    public Label4x3() {
        super();
        this.setSize(width * scale, height * scale);
        this.setImageableArea(leftMargin * scale, topMargin * scale, (width - leftMargin - rightMargin) * scale, (height - topMargin - bottomMargin) * scale);
    }
}
