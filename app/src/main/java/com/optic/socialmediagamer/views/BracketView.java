package com.optic.socialmediagamer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BracketView extends View {

    public static class Match {
        public final String player1;
        public final String player2;
        public Match(String p1, String p2) { player1 = p1; player2 = p2; }
    }

    private final List<Match> mMatches = new ArrayList<>();
    private final Paint mBoxPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNumPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int BOX_W  = 280;
    private static final int BOX_H  = 44;
    private static final int GAP_V  = 24;   // vertical gap between two players in same match
    private static final int GAP_M  = 40;   // vertical gap between matches
    private static final int PAD_L  = 16;
    private static final int PAD_T  = 24;

    public BracketView(Context ctx) { super(ctx); init(); }
    public BracketView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        mBoxPaint.setColor(Color.parseColor("#1A0055BB"));
        mBoxPaint.setStyle(Paint.Style.FILL);

        mLinePaint.setColor(Color.parseColor("#00F0FF"));
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(2f);

        mTextPaint.setColor(Color.parseColor("#E0E0E0"));
        mTextPaint.setTextSize(30f);

        mNumPaint.setColor(Color.parseColor("#00F0FF"));
        mNumPaint.setTextSize(26f);
        mNumPaint.setFakeBoldText(true);
    }

    public void setMatches(List<Match> matches) {
        mMatches.clear();
        mMatches.addAll(matches);
        int totalH = PAD_T * 2 + mMatches.size() * (BOX_H * 2 + GAP_V + GAP_M);
        setMinimumHeight(totalH);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int totalH = PAD_T * 2 + mMatches.size() * (BOX_H * 2 + GAP_V + GAP_M);
        setMeasuredDimension(w, Math.max(totalH, getSuggestedMinimumHeight()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int boxW = Math.min(BOX_W, getWidth() - PAD_L * 2 - 60);
        int y = PAD_T;

        for (int i = 0; i < mMatches.size(); i++) {
            Match m = mMatches.get(i);
            int x = PAD_L + 50;

            // Match number
            canvas.drawText("M" + (i + 1), PAD_L, y + BOX_H - 6, mNumPaint);

            // Player 1 box
            RectF r1 = new RectF(x, y, x + boxW, y + BOX_H);
            canvas.drawRoundRect(r1, 8, 8, mBoxPaint);
            canvas.drawRoundRect(r1, 8, 8, mLinePaint);
            drawClipped(canvas, m.player1, x + 10, y + BOX_H - 12, boxW - 20);

            // vs line
            float midX = x + boxW / 2f;
            canvas.drawLine(midX, y + BOX_H, midX, y + BOX_H + GAP_V, mLinePaint);
            canvas.drawText("vs", midX - 15, y + BOX_H + GAP_V - 4, mNumPaint);

            // Player 2 box
            int y2 = y + BOX_H + GAP_V;
            RectF r2 = new RectF(x, y2, x + boxW, y2 + BOX_H);
            canvas.drawRoundRect(r2, 8, 8, mBoxPaint);
            canvas.drawRoundRect(r2, 8, 8, mLinePaint);
            drawClipped(canvas, m.player2, x + 10, y2 + BOX_H - 12, boxW - 20);

            y += BOX_H * 2 + GAP_V + GAP_M;
        }
    }

    private void drawClipped(Canvas canvas, String text, float x, float y, int maxWidth) {
        float textW = mTextPaint.measureText(text);
        if (textW > maxWidth) {
            int len = text.length();
            while (len > 1 && mTextPaint.measureText(text, 0, len) + mTextPaint.measureText("…") > maxWidth) len--;
            text = text.substring(0, len) + "…";
        }
        canvas.drawText(text, x, y, mTextPaint);
    }
}
