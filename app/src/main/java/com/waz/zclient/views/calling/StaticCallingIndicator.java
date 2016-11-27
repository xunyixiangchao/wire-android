/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.views.calling;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import com.waz.api.AccentColor;
import com.waz.zclient.R;
import com.waz.zclient.ui.animation.interpolators.penner.Back;
import com.waz.zclient.ui.utils.ResourceUtils;
import com.waz.zclient.utils.ViewUtils;

public class StaticCallingIndicator extends View {

    public static final Property<StaticCallingIndicator, Float> INNER_RADIUS = new Property<StaticCallingIndicator, Float>(Float.class, "innerRadiusScale") {
        @Override
        public Float get(StaticCallingIndicator object) {
            return object.getInnerRadiusScale();
        }

        @Override
        public void set(StaticCallingIndicator object, Float value) {
            object.setInnerRadiusScale(value);
        }
    };
    public static final Property<StaticCallingIndicator, Float> MIDDLE_RADIUS = new Property<StaticCallingIndicator, Float>(Float.class, "middleRadiusScale") {
        @Override
        public Float get(StaticCallingIndicator object) {
            return object.getMiddleRadiusScale();
        }

        @Override
        public void set(StaticCallingIndicator object, Float value) {
            object.setMiddleRadiusScale(value);
        }
    };
    public static final Property<StaticCallingIndicator, Float> OUTER_RADIUS = new Property<StaticCallingIndicator, Float>(Float.class, "outerRadiusScale") {
        @Override
        public Float get(StaticCallingIndicator object) {
            return object.getOuterRadiusScale();
        }

        @Override
        public void set(StaticCallingIndicator object, Float value) {
            object.setOuterRadiusScale(value);
        }
    };
    private static final int DEFAULT_STROKE_WIDTH_DP = 1;
    private static final int DEFAULT_COLOR = Color.WHITE;

    private static final int DEFAULT_OPACITY = 100;
    private static final int DEFAULT_RADIUS = 0;

    private int outerOpacity;
    private int middleOpacity;
    private int innerOpacity;

    private int centerX;
    private int centerY;

    private Paint paint1;
    private Paint paint2;
    private Paint paint3;

    private int innerRadius;
    private int middleRadius;
    private int outerRadius;

    private float innerRadiusScale = 1.0f;
    private float middleRadiusScale = 1.0f;
    private float outerRadiusScale = 1.0f;

    private int strokeWidth;
    private AnimatorSet animatorSet;

    public StaticCallingIndicator(Context context) {
        this(context, null);
    }

    public StaticCallingIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticCallingIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        strokeWidth = ViewUtils.toPx(getContext(), DEFAULT_STROKE_WIDTH_DP);

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CallingIndicator, 0, 0);
        try {
            outerOpacity = a.getInt(R.styleable.CallingIndicator_outerOpacity, DEFAULT_OPACITY);
            middleOpacity = a.getInt(R.styleable.CallingIndicator_middleOpacity, DEFAULT_OPACITY);
            innerOpacity = a.getInt(R.styleable.CallingIndicator_innerOpacity, DEFAULT_OPACITY);

            outerRadius = a.getDimensionPixelSize(R.styleable.CallingIndicator_outerRadius, DEFAULT_RADIUS) - strokeWidth / 2;
            middleRadius = a.getDimensionPixelSize(R.styleable.CallingIndicator_middleRadius, DEFAULT_RADIUS) - strokeWidth / 2;
            innerRadius = a.getDimensionPixelOffset(R.styleable.CallingIndicator_innerRadius, DEFAULT_RADIUS) - strokeWidth / 2;

        } finally {
            a.recycle();
        }

        paint1 = getDefaultPaint();
        paint2 = getDefaultPaint();
        paint3 = getDefaultPaint();

        setColor(DEFAULT_COLOR);
    }

    public void setColor(AccentColor color) {
        setColor(color.getColor());
    }

    public void setColor(int color) {
        paint1.setColor(color);
        paint2.setColor(color);
        paint3.setColor(color);

        paint1.setAlpha(ViewUtils.getAlphaValue(innerOpacity));
        paint2.setAlpha(ViewUtils.getAlphaValue(middleOpacity));
        paint3.setAlpha(ViewUtils.getAlphaValue(outerOpacity));

        invalidate();
    }

    public void setFillRings(boolean fill) {
        paint1.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        paint2.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
        paint3.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, outerRadius * outerRadiusScale, paint3);
        canvas.drawCircle(centerX, centerY, middleRadius * middleRadiusScale, paint2);
        canvas.drawCircle(centerX, centerY, innerRadius * innerRadiusScale, paint1);
    }

    public void setInnerRadiusScale(float innerRadiusScale) {
        this.innerRadiusScale = innerRadiusScale;
        invalidate();
    }

    public void setMiddleRadiusScale(float middleRadiusScale) {
        this.middleRadiusScale = middleRadiusScale;
        invalidate();
    }

    public void setOuterRadiusScale(float outerRadiusScale) {
        this.outerRadiusScale = outerRadiusScale;
        invalidate();
    }

    public float getInnerRadiusScale() {
        return innerRadiusScale;
    }

    public float getMiddleRadiusScale() {
        return middleRadiusScale;
    }

    public float getOuterRadiusScale() {
        return outerRadiusScale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int actualWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int actualHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom() - getPaddingTop();

        centerX = getPaddingLeft() + actualWidth / 2;
        centerY = getPaddingTop() + actualHeight / 2;
    }

    private Paint getDefaultPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        return p;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimation();
    }

    public void runAnimation() {
        Context context = getContext();
        final long duration = context.getResources().getInteger(R.integer.animation_duration_long);
        final long animationCircleDelay = context.getResources().getInteger(R.integer.row_conversation__missed_call__animation__delay_factor);
        final float overshoot = ResourceUtils.getResourceFloat(context.getResources(), R.dimen.row_conversation__missed_call__animation__overshoot);

        ObjectAnimator collapseInnerRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.INNER_RADIUS, 1.0f, 0.5f);
        collapseInnerRadiusAnimator.setDuration(duration);
        collapseInnerRadiusAnimator.setStartDelay(2 * animationCircleDelay);
        collapseInnerRadiusAnimator.setInterpolator(new Back.EaseIn(overshoot));

        ObjectAnimator expandInnerRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.INNER_RADIUS, 0.5f, 1.0f);
        expandInnerRadiusAnimator.setDuration(duration);
        expandInnerRadiusAnimator.setInterpolator(new Back.EaseOut(overshoot));

        AnimatorSet innerRadiusSet = new AnimatorSet();
        innerRadiusSet.playSequentially(collapseInnerRadiusAnimator, expandInnerRadiusAnimator);

        ObjectAnimator collapseMiddleRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.MIDDLE_RADIUS, 1.0f, 0.5f);
        collapseMiddleRadiusAnimator.setDuration(duration);
        collapseMiddleRadiusAnimator.setStartDelay(animationCircleDelay);
        collapseMiddleRadiusAnimator.setInterpolator(new Back.EaseIn(overshoot));

        ObjectAnimator expandMiddleRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.MIDDLE_RADIUS, 0.5f, 1.0f);
        expandMiddleRadiusAnimator.setDuration(duration);
        expandMiddleRadiusAnimator.setStartDelay(2 * animationCircleDelay);
        expandMiddleRadiusAnimator.setInterpolator(new Back.EaseOut(overshoot));

        AnimatorSet middleRadiusSet = new AnimatorSet();
        middleRadiusSet.playSequentially(collapseMiddleRadiusAnimator, expandMiddleRadiusAnimator);

        ObjectAnimator collapseOuterRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.OUTER_RADIUS, 1.0f, 0.5f);
        collapseOuterRadiusAnimator.setDuration(duration);
        collapseOuterRadiusAnimator.setInterpolator(new Back.EaseIn(overshoot));

        ObjectAnimator expandOuterRadiusAnimator = ObjectAnimator.ofFloat(this, StaticCallingIndicator.OUTER_RADIUS, 0.5f, 1.0f);
        expandOuterRadiusAnimator.setStartDelay(4 * animationCircleDelay);
        expandOuterRadiusAnimator.setDuration(duration);
        expandOuterRadiusAnimator.setInterpolator(new Back.EaseOut(overshoot));

        AnimatorSet outerRadiusSet = new AnimatorSet();
        outerRadiusSet.playSequentially(collapseOuterRadiusAnimator, expandOuterRadiusAnimator);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(innerRadiusSet, middleRadiusSet, outerRadiusSet);
        animatorSet.setStartDelay(context.getResources().getInteger(R.integer.animation_delay_very_long));
        animatorSet.start();
    }

    public void cancelAnimation() {
        if (animatorSet == null) {
            return;
        }
        animatorSet.cancel();
        animatorSet = null;
    }
}
