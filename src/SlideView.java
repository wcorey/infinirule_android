package nz.gen.geek_central.infinirule;
/*
    Slide-rule display widget
*/

import android.graphics.PointF;
import android.graphics.Paint;
import android.view.MotionEvent;

public class SlideView extends android.view.View
  {
    private Scales.Scale UpperScale, LowerScale;
    private double UpperScaleOffset, LowerScaleOffset; /* (-1.0 .. 0.0] */
    private float CursorX; /* view x-coordinate */
    private int ScaleLength; /* in pixels */

    public void Reset()
      {
        UpperScaleOffset = 0.0;
        LowerScaleOffset = 0.0;
        CursorX = 0.0f;
        if (ScaleLength > 0)
          {
            ScaleLength = getWidth();
          } /*if*/
        invalidate();
      } /*Reset*/

    private void Init()
      /* common code for all constructors */
      {
        UpperScale = Scales.DefaultScale();
        LowerScale = UpperScale;
        ScaleLength = -1; /* proper value deferred to onLayout */
        Reset();
      } /*Init*/

    public SlideView
      (
        android.content.Context Context
      )
      {
        super(Context);
        Init();
      } /*SlideView*/

    public SlideView
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes
      )
      {
        this(Context, Attributes, 0);
      } /*SlideView*/

    public SlideView
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes,
        int DefaultStyle
      )
      {
        super(Context, Attributes, DefaultStyle);
        Init();
      } /*SlideView*/

    @Override
    protected void onLayout
      (
        boolean Changed,
        int Left,
        int Top,
        int Right,
        int Bottom
      )
      /* just a place to finish initialization after I know what my layout will be */
      {
        super.onLayout(Changed, Left, Top, Right, Bottom);
        if (ScaleLength < 0)
          {
            ScaleLength = getWidth();
          } /*if*/
      } /*onLayout*/

    public void SetScale
      (
        String NewScaleName,
        boolean Upper
      )
      {
        final Scales.Scale NewScale = Scales.KnownScales.get(NewScaleName);
        if (Upper)
          {
            UpperScale = NewScale;
            UpperScaleOffset = 0.0;
          }
        else
          {
            LowerScale = NewScale;
            LowerScaleOffset = 0.0;
          } /*if*/
        CursorX = 0.0f;
        ScaleLength = getWidth();
        invalidate();
      } /*SetScale*/

/*
    Mapping between image coordinates and view coordinates
*/

    public float ScaleToView
      (
        double Pos, /* [0.0 .. 1.0) */
        double Size,
        double Offset
      )
      /* returns a position on a scale, offset by the given amount,
        converted to view coordinates. */
      {
        return
            (float)((Pos + Offset) * Size * ScaleLength);
      } /*ScaleToView*/

    public double ViewToScale
      (
        float Coord,
        double Size,
        double Offset
      )
      /* returns a view coordinate converted to the corresponding
        position on a scale offset by the given amount. */
      {
        return
            Coord / Size / ScaleLength - Offset;
      } /*ViewToScale*/

    public double FindScaleOffset
      (
        float Coord,
        double Size,
        double Pos
      )
      /* finds the offset value such that the specified view coordinate
        maps to the specified position on a scale. */
      {
        final double Offset = Coord / Size / ScaleLength - Pos;
        return
            Offset - Math.ceil(Offset);
      } /*FindScaleOffset*/

/*
    Drawing
*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas g
      )
      {
        g.drawColor(0xfffffada);
        Scales.DrawLabel
          (
            /*g =*/ g,
            /*TheScale =*/ UpperScale,
            /*Upper =*/ true,
            /*Pos =*/ new PointF(getWidth() / 2.0f, getHeight() * 0.25f),
            /*Alignment =*/ Paint.Align.CENTER,
            /*Color =*/ 0xff000000
          );
        Scales.DrawLabel
          (
            /*g =*/ g,
            /*TheScale =*/ LowerScale,
            /*Upper =*/ false,
            /*Pos =*/ new PointF(getWidth() / 2.0f, getHeight() * 0.75f),
            /*Alignment =*/ Paint.Align.CENTER,
            /*Color =*/ 0xff000000
          );
        g.save(android.graphics.Canvas.MATRIX_SAVE_FLAG);
        final android.graphics.Matrix m1 = g.getMatrix();
        final android.graphics.Matrix m2 = g.getMatrix();
        for (boolean Upper = false;;)
          {
            final android.graphics.Matrix m = Upper ? m1 : m2;
            final Scales.Scale TheScale = Upper ? UpperScale : LowerScale;
            final int ScaleRepeat =
                    (getWidth() + (int)(ScaleLength * TheScale.Size() - 1))
                /
                    (int)(ScaleLength * TheScale.Size());
            m.preTranslate
              (
                (float)(
                    (Upper ? UpperScaleOffset : LowerScaleOffset) * ScaleLength * TheScale.Size()
                ),
                getHeight() / 2.0f
              );
            for (int i = -1; i <= ScaleRepeat; ++i)
              {
                g.setMatrix(m);
                TheScale.Draw(g, (float)(ScaleLength * TheScale.Size()), !Upper);
                m.preTranslate((float)(ScaleLength * TheScale.Size()), 0.0f);
              } /*for*/
            if (Upper)
                break;
            Upper = true;
          } /*for*/
        g.restore();
          {
            final float CursorLeft = CursorX - Scales.HalfCursorWidth;
            final float CursorRight = CursorX + Scales.HalfCursorWidth;
            final Paint CursorHow = new Paint();
            g.drawLine(CursorX, 0.0f, CursorX, getHeight(), CursorHow);
            CursorHow.setStyle(Paint.Style.FILL);
            CursorHow.setColor(0x20202020);
            g.drawRect
              (
                /*left =*/ CursorLeft,
                /*top =*/ 0.0f,
                /*right =*/ CursorRight,
                /*bottom =*/ getHeight(),
                /*paint =*/ CursorHow
              );
            CursorHow.setStyle(Paint.Style.STROKE);
            CursorHow.setColor(0x80808080);
            g.drawLine(CursorLeft, 0.0f, CursorLeft, getHeight(), CursorHow);
            g.drawLine(CursorRight, 0.0f, CursorRight, getHeight(), CursorHow);
          }
      } /*onDraw*/

/*
    Interaction handling
*/

    private PointF
        LastMouse1 = null,
        LastMouse2 = null;
    private int
        Mouse1ID = -1,
        Mouse2ID = -1;
    private enum MovingState
      {
        MovingNothing,
        MovingCursor,
        MovingBothScales,
        MovingLowerScale,
      } /*MovingState*/
    private MovingState MovingWhat = MovingState.MovingNothing;
    private boolean PrecisionMove = false;
    private final float PrecisionFactor = 10.0f;

    @Override
    public boolean onTouchEvent
      (
        MotionEvent TheEvent
      )
      {
        boolean Handled = false;
        switch (TheEvent.getAction() & (1 << MotionEvent.ACTION_POINTER_ID_SHIFT) - 1)
          {
        case MotionEvent.ACTION_DOWN:
            LastMouse1 = new PointF(TheEvent.getX(), TheEvent.getY());
            Mouse1ID = TheEvent.getPointerId(0);
            if
              (
                    CursorX - Scales.HalfCursorWidth <= LastMouse1.x
                &&
                    LastMouse1.x < CursorX + Scales.HalfCursorWidth
              )
              {
                MovingWhat = MovingState.MovingCursor;
              }
            else if (LastMouse1.y > getHeight() / 2.0f)
              {
                MovingWhat = MovingState.MovingLowerScale;
              }
            else
              {
                MovingWhat = MovingState.MovingBothScales;
              } /*if*/
            PrecisionMove = Math.abs(LastMouse1.y - getHeight() / 2.0f) / getHeight() < 0.2f;
            Handled = true;
        break;
        case MotionEvent.ACTION_POINTER_DOWN:
            if
              (
                    MovingWhat == MovingState.MovingLowerScale
                ||
                    MovingWhat == MovingState.MovingBothScales
              )
              {
                final int PointerIndex =
                        (TheEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                    >>
                        MotionEvent.ACTION_POINTER_ID_SHIFT;
                final int MouseID = TheEvent.getPointerId(PointerIndex);
                final PointF MousePos = new PointF
                  (
                    TheEvent.getX(PointerIndex),
                    TheEvent.getY(PointerIndex)
                  );
                if (LastMouse1 == null)
                  {
                    Mouse1ID = MouseID;
                    LastMouse1 = MousePos;
                  }
                else if (LastMouse2 == null)
                  {
                    Mouse2ID = MouseID;
                    LastMouse2 = MousePos;
                  } /*if*/
              } /*if*/
            Handled = true;
        break;
        case MotionEvent.ACTION_MOVE:
            if (LastMouse1 != null)
              {
                final int Mouse1Index = TheEvent.findPointerIndex(Mouse1ID);
                final int Mouse2Index =
                    LastMouse2 != null ?
                        TheEvent.findPointerIndex(Mouse2ID)
                    :
                        -1;
                if (Mouse1Index >= 0 || Mouse2Index >= 0)
                  {
                    final PointF ThisMouse1 =
                        Mouse1Index >= 0 ?
                            new PointF
                              (
                                TheEvent.getX(Mouse1Index),
                                TheEvent.getY(Mouse1Index)
                              )
                        :
                            null;
                    final PointF ThisMouse2 =
                        Mouse2Index >= 0 ?
                            new PointF
                             (
                               TheEvent.getX(Mouse2Index),
                               TheEvent.getY(Mouse2Index)
                             )
                         :
                            null;
                    if (ThisMouse1 != null || ThisMouse2 != null)
                      {
                        if
                          (
                                ThisMouse1 != null
                            &&
                                ThisMouse2 != null
                            &&
                                    ThisMouse1.y < getHeight() / 2.0f
                                !=
                                    ThisMouse2.y < getHeight() / 2.0f
                          )
                          {
                          /* simultaneous scrolling of both scales */
                            PointF
                                ThisMouseUpper, ThisMouseLower, LastMouseUpper, LastMouseLower;
                            if (ThisMouse1.y < getHeight() / 2.0f)
                              {
                                ThisMouseUpper = ThisMouse1;
                                LastMouseUpper = LastMouse1;
                                ThisMouseLower = ThisMouse2;
                                LastMouseLower = LastMouse2;
                              }
                            else
                              {
                                ThisMouseUpper = ThisMouse2;
                                LastMouseUpper = LastMouse2;
                                ThisMouseLower = ThisMouse1;
                                LastMouseLower = LastMouse1;
                              } /*if*/
                            if (PrecisionMove)
                              {
                                ThisMouseUpper = new PointF
                                  (
                                    LastMouseUpper.x + (ThisMouseUpper.x - LastMouseUpper.x) / PrecisionFactor,
                                    ThisMouseUpper.y
                                  );
                                ThisMouseLower = new PointF
                                  (
                                    LastMouseLower.x + (ThisMouseLower.x - LastMouseLower.x) / PrecisionFactor,
                                    ThisMouseLower.y
                                  );
                              } /*if*/
                            UpperScaleOffset =
                                FindScaleOffset
                                  (
                                    ThisMouseUpper.x,
                                    UpperScale.Size(),
                                    ViewToScale(LastMouseUpper.x, UpperScale.Size(), UpperScaleOffset)
                                  );
                            LowerScaleOffset =
                                FindScaleOffset
                                  (
                                    ThisMouseLower.x,
                                    LowerScale.Size(),
                                    ViewToScale(LastMouseLower.x, LowerScale.Size(), LowerScaleOffset)
                                  );
                            invalidate();
                          }
                        else
                          {
                            PointF ThisMouse =
                                ThisMouse1 != null ?
                                    ThisMouse2 != null ?
                                        new PointF
                                          (
                                            (ThisMouse1.x + ThisMouse2.x) / 2.0f,
                                            (ThisMouse1.y + ThisMouse2.y) / 2.0f
                                          )
                                    :
                                        ThisMouse1
                                :
                                    ThisMouse2;
                            final PointF LastMouse =
                                ThisMouse1 != null ?
                                    ThisMouse2 != null ?
                                        new PointF
                                          (
                                            (LastMouse1.x + LastMouse2.x) / 2.0f,
                                            (LastMouse1.y + LastMouse2.y) / 2.0f
                                          )
                                    :
                                        LastMouse1
                                :
                                    LastMouse2;
                            if (PrecisionMove)
                              {
                                ThisMouse = new PointF
                                  (
                                    LastMouse.x + (ThisMouse.x - LastMouse.x) / PrecisionFactor,
                                    ThisMouse.y
                                  );
                              } /*if*/
                            switch (MovingWhat)
                              {
                            case MovingCursor:
                                CursorX = Math.max(0.0f, Math.min(CursorX + ThisMouse.x - LastMouse.x, getWidth()));
                                invalidate();
                            break;
                            case MovingBothScales:
                            case MovingLowerScale:
                                for (boolean Upper = false;;)
                                  {
                                    final double ScaleSize =
                                        ((Scales.Scale)(Upper ? UpperScale : LowerScale)).Size();
                                    final double NewOffset =
                                        FindScaleOffset
                                          (
                                            ThisMouse.x,
                                            ScaleSize,
                                            ViewToScale
                                              (
                                                LastMouse.x,
                                                ScaleSize,
                                                Upper ? UpperScaleOffset : LowerScaleOffset
                                              )
                                          );
                                    if (Upper)
                                      {
                                        UpperScaleOffset = NewOffset;
                                      }
                                    else
                                      {
                                        LowerScaleOffset = NewOffset;
                                      } /*if*/
                                    if (Upper || MovingWhat != MovingState.MovingBothScales)
                                        break;
                                    Upper = true;
                                  } /*for*/
                                invalidate();
                                if
                                  (
                                        ThisMouse1 != null
                                    &&
                                        ThisMouse2 != null
                                    &&
                                            ThisMouse1.y < getHeight() / 2.0f
                                        ==
                                            ThisMouse2.y < getHeight() / 2.0f
                                  )
                                  {
                                  /* pinch to zoom--note no PrecisionMove here */
                                    final float LastDistance = (float)Math.hypot
                                      (
                                        LastMouse1.x - LastMouse2.x,
                                        LastMouse1.y - LastMouse2.y
                                      );
                                    final float ThisDistance = (float)Math.hypot
                                      (
                                        ThisMouse1.x - ThisMouse2.x,
                                        ThisMouse1.y - ThisMouse2.y
                                      );
                                    if
                                      (
                                            LastDistance != 0.0f
                                        &&
                                            ThisDistance != 0.0f
                                      )
                                      {
                                        ScaleLength =
                                            (int)(
                                                ScaleLength * ThisDistance /  LastDistance
                                            );
                                        invalidate();
                                      } /*if*/
                                  } /*if*/
                            break;
                              } /*switch*/
                          } /*if*/
                        LastMouse1 = ThisMouse1;
                        LastMouse2 = ThisMouse2;
                      } /*if*/
                  } /*if*/
              } /*if*/
            Handled = true;
        break;
        case MotionEvent.ACTION_POINTER_UP:
            if (LastMouse2 != null)
              {
                final int PointerIndex =
                        (TheEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                    >>
                        MotionEvent.ACTION_POINTER_ID_SHIFT;
                final int PointerID = TheEvent.getPointerId(PointerIndex);
                if (PointerID == Mouse1ID)
                  {
                    Mouse1ID = Mouse2ID;
                    LastMouse1 = LastMouse2;
                    Mouse2ID = -1;
                    LastMouse2 = null;
                  }
                else if (PointerID == Mouse2ID)
                  {
                    Mouse2ID = -1;
                    LastMouse2 = null;
                  } /*if*/
              } /*if*/
            Handled = true;
        break;
        case MotionEvent.ACTION_UP:
            LastMouse1 = null;
            LastMouse2 = null;
            Mouse1ID = -1;
            Mouse2ID = -1;
            MovingWhat = MovingState.MovingNothing;
            Handled = true;
        break;
          } /*switch*/
        return
            Handled;
      } /*onTouchEvent*/

  } /*SlideView*/
