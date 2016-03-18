/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.widget.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.platform.test.annotations.Presubmit;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.cts.util.ViewTestUtils;

import static org.mockito.Mockito.*;

@SmallTest
public class ListPopupWindowTest extends
        ActivityInstrumentationTestCase2<MockPopupWindowCtsActivity> {
    private Instrumentation mInstrumentation;
    private Activity mActivity;

    private Builder mPopupWindowBuilder;

    /** The list popup window. */
    private ListPopupWindow mPopupWindow;

    private AdapterView.OnItemClickListener mItemClickListener;

    /**
     * Item click listener that dismisses our <code>ListPopupWindow</code> when any item
     * is clicked. Note that this needs to be a separate class that is also protected (not
     * private) so that Mockito can "spy" on it.
     */
    protected class PopupItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            mPopupWindow.dismiss();
        }
    }

    /**
     * Instantiates a new popup window test.
     */
    public ListPopupWindowTest() {
        super(MockPopupWindowCtsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mItemClickListener = new PopupItemClickListener();
    }

    @Override
    protected void tearDown() throws Exception {
        if ((mPopupWindowBuilder != null) && (mPopupWindow != null)) {
            mPopupWindowBuilder.dismiss();
        }

        super.tearDown();
    }

    public void testConstructor() {
        new ListPopupWindow(mActivity);

        new ListPopupWindow(mActivity, null);

        new ListPopupWindow(mActivity, null, android.R.attr.popupWindowStyle);

        new ListPopupWindow(mActivity, null, 0, android.R.style.Widget_Material_ListPopupWindow);
    }

    public void testAccessBackground() {
        mPopupWindowBuilder = new Builder();
        mPopupWindowBuilder.show();

        Drawable drawable = new ColorDrawable();
        mPopupWindow.setBackgroundDrawable(drawable);
        assertSame(drawable, mPopupWindow.getBackground());

        mPopupWindow.setBackgroundDrawable(null);
        assertNull(mPopupWindow.getBackground());
    }

    public void testAccessAnimationStyle() {
        mPopupWindowBuilder = new Builder();
        mPopupWindowBuilder.show();
        assertEquals(0, mPopupWindow.getAnimationStyle());

        mPopupWindow.setAnimationStyle(android.R.style.Animation_Toast);
        assertEquals(android.R.style.Animation_Toast, mPopupWindow.getAnimationStyle());

        // abnormal values
        mPopupWindow.setAnimationStyle(-100);
        assertEquals(-100, mPopupWindow.getAnimationStyle());
    }

    public void testAccessHeight() {
        mPopupWindowBuilder = new Builder();
        mPopupWindowBuilder.show();

        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, mPopupWindow.getHeight());

        int height = getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = getDisplay().getHeight();
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        mPopupWindow.setHeight(0);
        assertEquals(0, mPopupWindow.getHeight());

        height = getDisplay().getHeight() * 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());

        height = -getDisplay().getHeight() / 2;
        mPopupWindow.setHeight(height);
        assertEquals(height, mPopupWindow.getHeight());
    }

    /**
     * Gets the display.
     *
     * @return the display
     */
    private Display getDisplay() {
        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }

    public void testAccessWidth() {
        mPopupWindowBuilder = new Builder().ignoreContentWidth();
        mPopupWindowBuilder.show();

        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT, mPopupWindow.getWidth());

        int width = getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = getDisplay().getWidth();
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        mPopupWindow.setWidth(0);
        assertEquals(0, mPopupWindow.getWidth());

        width = getDisplay().getWidth() * 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());

        width = - getDisplay().getWidth() / 2;
        mPopupWindow.setWidth(width);
        assertEquals(width, mPopupWindow.getWidth());
    }

    private void verifyAnchoring(int horizontalOffset, int verticalOffset, int gravity) {
        final View upperAnchor = mActivity.findViewById(R.id.anchor_upper);
        final ListView listView = mPopupWindow.getListView();
        int[] anchorXY = new int[2];
        int[] listViewOnScreenXY = new int[2];
        int[] listViewInWindowXY = new int[2];

        assertTrue(mPopupWindow.isShowing());
        assertEquals(upperAnchor, mPopupWindow.getAnchorView());

        listView.getLocationOnScreen(listViewOnScreenXY);
        upperAnchor.getLocationOnScreen(anchorXY);
        listView.getLocationInWindow(listViewInWindowXY);

        int expectedListViewOnScreenX = anchorXY[0] + listViewInWindowXY[0] + horizontalOffset;
        final int absoluteGravity =
                Gravity.getAbsoluteGravity(gravity, upperAnchor.getLayoutDirection());
        if (absoluteGravity == Gravity.RIGHT) {
            expectedListViewOnScreenX -= (listView.getWidth() - upperAnchor.getWidth());
        }
        int expectedListViewOnScreenY = anchorXY[1] + listViewInWindowXY[1]
                + upperAnchor.getHeight() + verticalOffset;
        assertEquals(expectedListViewOnScreenX, listViewOnScreenXY[0]);
        assertEquals(expectedListViewOnScreenY, listViewOnScreenXY[1]);
    }

    public void testAnchoring() {
        mPopupWindowBuilder = new Builder();
        mPopupWindowBuilder.show();

        assertEquals(0, mPopupWindow.getHorizontalOffset());
        assertEquals(0, mPopupWindow.getVerticalOffset());

        verifyAnchoring(0, 0, Gravity.NO_GRAVITY);
    }

    public void testAnchoringWithHorizontalOffset() {
        mPopupWindowBuilder = new Builder().withHorizontalOffset(50);
        mPopupWindowBuilder.show();

        assertEquals(50, mPopupWindow.getHorizontalOffset());
        assertEquals(0, mPopupWindow.getVerticalOffset());

        verifyAnchoring(50, 0, Gravity.NO_GRAVITY);
    }

    public void testAnchoringWithVerticalOffset() {
        mPopupWindowBuilder = new Builder().withVerticalOffset(60);
        mPopupWindowBuilder.show();

        assertEquals(0, mPopupWindow.getHorizontalOffset());
        assertEquals(60, mPopupWindow.getVerticalOffset());

        verifyAnchoring(0, 60, Gravity.NO_GRAVITY);
    }

    public void testAnchoringWithRightGravity() {
        mPopupWindowBuilder = new Builder().withDropDownGravity(Gravity.RIGHT);
        mPopupWindowBuilder.show();

        assertEquals(0, mPopupWindow.getHorizontalOffset());
        assertEquals(0, mPopupWindow.getVerticalOffset());

        verifyAnchoring(0, 0, Gravity.RIGHT);
    }

    public void testAnchoringWithEndGravity() {
        mPopupWindowBuilder = new Builder().withDropDownGravity(Gravity.END);
        mPopupWindowBuilder.show();

        assertEquals(0, mPopupWindow.getHorizontalOffset());
        assertEquals(0, mPopupWindow.getVerticalOffset());

        verifyAnchoring(0, 0, Gravity.END);
    }

    public void testSetWindowLayoutType() {
        mPopupWindowBuilder = new Builder().withWindowLayoutType(
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
        mPopupWindowBuilder.show();
        assertTrue(mPopupWindow.isShowing());

        WindowManager.LayoutParams p = (WindowManager.LayoutParams)
                mPopupWindow.getListView().getRootView().getLayoutParams();
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL, p.type);
    }

    public void testDismiss() {
        mPopupWindowBuilder = new Builder();
        assertFalse(mPopupWindow.isShowing());

        mPopupWindowBuilder.show();
        assertTrue(mPopupWindow.isShowing());

        mPopupWindowBuilder.dismiss();
        assertFalse(mPopupWindow.isShowing());

        mPopupWindowBuilder.dismiss();
        assertFalse(mPopupWindow.isShowing());
    }

    public void testSetOnDismissListener() {
        mPopupWindowBuilder = new Builder().withDismissListener();
        mPopupWindowBuilder.show();
        mPopupWindowBuilder.dismiss();
        verify(mPopupWindowBuilder.mOnDismissListener, times(1)).onDismiss();

        mPopupWindowBuilder.showAgain();
        mPopupWindowBuilder.dismiss();
        verify(mPopupWindowBuilder.mOnDismissListener, times(2)).onDismiss();

        mPopupWindow.setOnDismissListener(null);
        mPopupWindowBuilder.showAgain();
        mPopupWindowBuilder.dismiss();
        // Since we've reset the listener to null, we are not expecting any more interactions
        // on the previously registered listener.
        verifyNoMoreInteractions(mPopupWindowBuilder.mOnDismissListener);
    }

    public void testAccessInputMethodMode() {
        mPopupWindowBuilder = new Builder().withDismissListener();
        mPopupWindowBuilder.show();

        assertEquals(PopupWindow.INPUT_METHOD_NEEDED, mPopupWindow.getInputMethodMode());
        assertFalse(mPopupWindow.isInputMethodNotNeeded());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE);
        assertEquals(PopupWindow.INPUT_METHOD_FROM_FOCUSABLE, mPopupWindow.getInputMethodMode());
        assertFalse(mPopupWindow.isInputMethodNotNeeded());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NEEDED, mPopupWindow.getInputMethodMode());
        assertFalse(mPopupWindow.isInputMethodNotNeeded());

        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        assertEquals(PopupWindow.INPUT_METHOD_NOT_NEEDED, mPopupWindow.getInputMethodMode());
        assertTrue(mPopupWindow.isInputMethodNotNeeded());

        mPopupWindow.setInputMethodMode(-1);
        assertEquals(-1, mPopupWindow.getInputMethodMode());
        assertFalse(mPopupWindow.isInputMethodNotNeeded());
    }

    public void testAccessSoftInputMethodMode() {
        mPopupWindowBuilder = new Builder().withDismissListener();
        mPopupWindowBuilder.show();

        mPopupWindow = new ListPopupWindow(mActivity);
        assertEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED,
                mPopupWindow.getSoftInputMode());

        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
                mPopupWindow.getSoftInputMode());

        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        assertEquals(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE,
                mPopupWindow.getSoftInputMode());
    }

    private void verifyDismissalViaTouch(boolean setupAsModal) throws Throwable {
        // Register a click listener on the top-level container
        final View mainContainer = mActivity.findViewById(R.id.main_container);
        View.OnClickListener mockContainerClickListener = mock(View.OnClickListener.class);
        mainContainer.setOnClickListener(mockContainerClickListener);

        // Configure a list popup window with requested modality
        mPopupWindowBuilder = new Builder().setModal(setupAsModal).withDismissListener();
        mPopupWindowBuilder.show();

        assertTrue("Popup window showing", mPopupWindow.isShowing());
        // Make sure that the modality of the popup window is set up correctly
        assertEquals("Popup window modality", setupAsModal, mPopupWindow.isModal());

        // Determine the location of the popup on the screen so that we can emulate
        // a tap outside of its bounds to dismiss it
        final int[] popupOnScreenXY = new int[2];
        final Rect rect = new Rect();
        mPopupWindow.getListView().getLocationOnScreen(popupOnScreenXY);
        mPopupWindow.getBackground().getPadding(rect);

        int emulatedTapX = popupOnScreenXY[0] - rect.left - 20;
        int emulatedTapY = popupOnScreenXY[1] + mPopupWindow.getListView().getHeight() +
                rect.top + rect.bottom + 20;

        // The logic below uses Instrumentation to emulate a tap outside the bounds of the
        // displayed list popup window. This tap is then treated by the framework to be "split" as
        // the ACTION_OUTSIDE for the popup itself, as well as DOWN / MOVE / UP for the underlying
        // view root if the popup is not modal.
        // It is not correct to emulate these two sequences separately in the test, as it
        // wouldn't emulate the user-facing interaction for this test. Note that usage
        // of Instrumentation is necessary here since Espresso's actions operate at the level
        // of view or data. Also, we don't want to use View.dispatchTouchEvent directly as
        // that would require emulation of two separate sequences as well.

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        // Inject DOWN event
        long downTime = SystemClock.uptimeMillis();
        MotionEvent eventDown = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventDown);

        // Inject MOVE event
        long moveTime = SystemClock.uptimeMillis();
        MotionEvent eventMove = MotionEvent.obtain(
                moveTime, moveTime, MotionEvent.ACTION_MOVE, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventMove);

        // Inject UP event
        long upTime = SystemClock.uptimeMillis();
        MotionEvent eventUp = MotionEvent.obtain(
                upTime, upTime, MotionEvent.ACTION_UP, emulatedTapX, emulatedTapY, 1);
        instrumentation.sendPointerSync(eventUp);

        // Wait for the system to process all events in the queue
        instrumentation.waitForIdleSync();

        // At this point our popup should not be showing and should have notified its
        // dismiss listener
        verify(mPopupWindowBuilder.mOnDismissListener, times(1)).onDismiss();
        assertFalse("Popup window not showing after outside click", mPopupWindow.isShowing());

        // Also test that the click outside the popup bounds has been "delivered" to the main
        // container only if the popup is not modal
        verify(mockContainerClickListener, times(setupAsModal ? 0 : 1)).onClick(mainContainer);
    }

    public void testDismissalOutsideNonModal() throws Throwable {
        verifyDismissalViaTouch(false);
    }

    public void testDismissalOutsideModal() throws Throwable {
        verifyDismissalViaTouch(true);
    }

    public void testItemClicks() throws Throwable {
        mPopupWindowBuilder = new Builder().withItemClickListener().withDismissListener();
        mPopupWindowBuilder.show();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPopupWindow.performItemClick(2);
            }
        });
        mInstrumentation.waitForIdleSync();

        verify(mPopupWindowBuilder.mOnItemClickListener, times(1)).onItemClick(
                any(AdapterView.class), any(View.class), eq(2), eq(2L));
        // Also verify that the popup window has been dismissed
        assertFalse(mPopupWindow.isShowing());
        verify(mPopupWindowBuilder.mOnDismissListener, times(1)).onDismiss();

        mPopupWindowBuilder.showAgain();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPopupWindow.getListView().performItemClick(null, 1, 1);
            }
        });
        mInstrumentation.waitForIdleSync();

        verify(mPopupWindowBuilder.mOnItemClickListener, times(1)).onItemClick(
                any(AdapterView.class), any(View.class), eq(1), eq(1L));
        // Also verify that the popup window has been dismissed
        assertFalse(mPopupWindow.isShowing());
        verify(mPopupWindowBuilder.mOnDismissListener, times(2)).onDismiss();

        // Finally verify that our item click listener has only been called twice
        verifyNoMoreInteractions(mPopupWindowBuilder.mOnItemClickListener);
    }

    public void testPromptViewAbove() throws Throwable {
        final View promptView = LayoutInflater.from(mActivity).inflate(
                R.layout.popupwindow_prompt, null);
        mPopupWindowBuilder = new Builder().withPrompt(
                promptView, ListPopupWindow.POSITION_PROMPT_ABOVE);
        mPopupWindowBuilder.show();

        // Verify that our prompt is displayed on the screen and is above the first list item
        assertTrue(promptView.isAttachedToWindow());
        assertTrue(promptView.isShown());
        assertEquals(ListPopupWindow.POSITION_PROMPT_ABOVE, mPopupWindow.getPromptPosition());

        final int[] promptViewOnScreenXY = new int[2];
        promptView.getLocationOnScreen(promptViewOnScreenXY);

        final ListView listView = mPopupWindow.getListView();
        final View firstListChild = listView.getChildAt(0);
        final int[] firstChildOnScreenXY = new int[2];
        firstListChild.getLocationOnScreen(firstChildOnScreenXY);

        assertTrue(promptViewOnScreenXY[1] + promptView.getHeight() <= firstChildOnScreenXY[1]);
    }

    public void testPromptViewBelow() throws Throwable {
        final View promptView = LayoutInflater.from(mActivity).inflate(
                R.layout.popupwindow_prompt, null);
        mPopupWindowBuilder = new Builder().withPrompt(
                promptView, ListPopupWindow.POSITION_PROMPT_BELOW);
        mPopupWindowBuilder.show();

        // Verify that our prompt is displayed on the screen and is below the last list item
        assertTrue(promptView.isAttachedToWindow());
        assertTrue(promptView.isShown());
        assertEquals(ListPopupWindow.POSITION_PROMPT_BELOW, mPopupWindow.getPromptPosition());

        final int[] promptViewOnScreenXY = new int[2];
        promptView.getLocationOnScreen(promptViewOnScreenXY);

        final ListView listView = mPopupWindow.getListView();
        final View lastListChild = listView.getChildAt(listView.getChildCount() - 1);
        final int[] lastChildOnScreenXY = new int[2];
        lastListChild.getLocationOnScreen(lastChildOnScreenXY);

        assertTrue(lastChildOnScreenXY[1] + lastListChild.getHeight() <= promptViewOnScreenXY[1]);
    }

    @Presubmit
    public void testAccessSelection() throws Throwable {
        mPopupWindowBuilder = new Builder().withItemSelectedListener();
        mPopupWindowBuilder.show();

        final ListView listView = mPopupWindow.getListView();

        // Select an item
        ViewTestUtils.runOnMainAndDrawSync(mInstrumentation, listView, new Runnable() {
            public void run() {
                mPopupWindow.setSelection(1);
            }
        });

        // And verify the current selection state + selection listener invocation
        verify(mPopupWindowBuilder.mOnItemSelectedListener, times(1)).onItemSelected(
                any(AdapterView.class), any(View.class), eq(1), eq(1L));
        assertEquals(1, mPopupWindow.getSelectedItemId());
        assertEquals(1, mPopupWindow.getSelectedItemPosition());
        assertEquals("Bob", mPopupWindow.getSelectedItem());
        View selectedView = mPopupWindow.getSelectedView();
        assertNotNull(selectedView);
        assertEquals("Bob",
                ((TextView) selectedView.findViewById(android.R.id.text1)).getText());

        // Select another item
        ViewTestUtils.runOnMainAndDrawSync(mInstrumentation, listView, new Runnable() {
            public void run() {
                mPopupWindow.setSelection(3);
            }
        });

        // And verify the new selection state + selection listener invocation
        verify(mPopupWindowBuilder.mOnItemSelectedListener, times(1)).onItemSelected(
                any(AdapterView.class), any(View.class), eq(3), eq(3L));
        assertEquals(3, mPopupWindow.getSelectedItemId());
        assertEquals(3, mPopupWindow.getSelectedItemPosition());
        assertEquals("Deirdre", mPopupWindow.getSelectedItem());
        selectedView = mPopupWindow.getSelectedView();
        assertNotNull(selectedView);
        assertEquals("Deirdre",
                ((TextView) selectedView.findViewById(android.R.id.text1)).getText());

        // Clear selection
        ViewTestUtils.runOnMainAndDrawSync(mInstrumentation, listView, new Runnable() {
            public void run() {
                mPopupWindow.clearListSelection();
            }
        });

        // And verify empty selection state + no more selection listener invocation
        verify(mPopupWindowBuilder.mOnItemSelectedListener, times(1)).onNothingSelected(
                any(AdapterView.class));
        assertEquals(AdapterView.INVALID_ROW_ID, mPopupWindow.getSelectedItemId());
        assertEquals(AdapterView.INVALID_POSITION, mPopupWindow.getSelectedItemPosition());
        assertEquals(null, mPopupWindow.getSelectedItem());
        assertEquals(null, mPopupWindow.getSelectedView());
        verifyNoMoreInteractions(mPopupWindowBuilder.mOnItemSelectedListener);
    }

    /**
     * Inner helper class to configure an instance of <code>ListPopupWindow</code> for the
     * specific test. The main reason for its existence is that once a popup window is shown
     * with the show() method, most of its configuration APIs are no-ops. This means that
     * we can't add logic that is specific to a certain test (such as dismissing a non-modal
     * popup window) once it's shown and we have a reference to a displayed ListPopupWindow.
     */
    public class Builder {
        private boolean mIsModal;
        private boolean mHasDismissListener;
        private boolean mHasItemClickListener;
        private boolean mHasItemSelectedListener;
        private boolean mIgnoreContentWidth;
        private int mHorizontalOffset;
        private int mVerticalOffset;
        private int mDropDownGravity;

        private boolean mHasWindowLayoutType;
        private int mWindowLayoutType;

        private View mPromptView;
        private int mPromptPosition;

        private AdapterView.OnItemClickListener mOnItemClickListener;
        private AdapterView.OnItemSelectedListener mOnItemSelectedListener;
        private PopupWindow.OnDismissListener mOnDismissListener;

        public Builder() {
            mPopupWindow = new ListPopupWindow(mActivity);
        }

        public Builder ignoreContentWidth() {
            mIgnoreContentWidth = true;
            return this;
        }

        public Builder setModal(boolean isModal) {
            mIsModal = isModal;
            return this;
        }

        public Builder withItemClickListener() {
            mHasItemClickListener = true;
            return this;
        }

        public Builder withItemSelectedListener() {
            mHasItemSelectedListener = true;
            return this;
        }

        public Builder withDismissListener() {
            mHasDismissListener = true;
            return this;
        }

        public Builder withWindowLayoutType(int windowLayoutType) {
            mHasWindowLayoutType = true;
            mWindowLayoutType = windowLayoutType;
            return this;
        }

        public Builder withHorizontalOffset(int horizontalOffset) {
            mHorizontalOffset = horizontalOffset;
            return this;
        }

        public Builder withVerticalOffset(int verticalOffset) {
            mVerticalOffset = verticalOffset;
            return this;
        }

        public Builder withDropDownGravity(int dropDownGravity) {
            mDropDownGravity = dropDownGravity;
            return this;
        }

        public Builder withPrompt(View promptView, int promptPosition) {
            mPromptView = promptView;
            mPromptPosition = promptPosition;
            return this;
        }

        private int getContentWidth(ListAdapter listAdapter, Drawable background) {
            if (listAdapter == null) {
                return 0;
            }

            int width = 0;
            View itemView = null;
            int itemType = 0;

            for (int i = 0; i < listAdapter.getCount(); i++) {
                final int positionType = listAdapter.getItemViewType(i);
                if (positionType != itemType) {
                    itemType = positionType;
                    itemView = null;
                }
                itemView = listAdapter.getView(i, itemView, null);
                if (itemView.getLayoutParams() == null) {
                    itemView.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                width = Math.max(width, itemView.getMeasuredWidth());
            }

            // Add background padding to measured width
            if (background != null) {
                final Rect rect = new Rect();
                background.getPadding(rect);
                width += rect.left + rect.right;
            }

            return width;
        }

        private void show() {
            final String[] POPUP_CONTENT =
                    new String[]{"Alice", "Bob", "Charlie", "Deirdre", "El"};
            final BaseAdapter listPopupAdapter = new BaseAdapter() {
                class ViewHolder {
                    private TextView title;
                }

                @Override
                public int getCount() {
                    return POPUP_CONTENT.length;
                }

                @Override
                public Object getItem(int position) {
                    return POPUP_CONTENT[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(mActivity).inflate(
                                android.R.layout.simple_list_item_1, parent, false);
                        ViewHolder viewHolder = new ViewHolder();
                        viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
                        convertView.setTag(viewHolder);
                    }

                    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                    viewHolder.title.setText(POPUP_CONTENT[position]);
                    return convertView;
                }
            };

            mPopupWindow.setAdapter(listPopupAdapter);
            mPopupWindow.setAnchorView(mActivity.findViewById(R.id.anchor_upper));

            // The following mock listeners have to be set before the call to show() as
            // they are set on the internally constructed drop down.
            if (mHasItemClickListener) {
                // Wrap our item click listener with a Mockito spy
                mOnItemClickListener = spy(mItemClickListener);
                // Register that spy as the item click listener on the ListPopupWindow
                mPopupWindow.setOnItemClickListener(mOnItemClickListener);
                // And configure Mockito to call our original listener with onItemClick.
                // This way we can have both our item click listener running to dismiss the popup
                // window, and track the invocations of onItemClick with Mockito APIs.
                doCallRealMethod().when(mOnItemClickListener).onItemClick(
                        any(AdapterView.class), any(View.class), any(int.class), any(int.class));
            }

            if (mHasItemSelectedListener) {
                mOnItemSelectedListener = mock(AdapterView.OnItemSelectedListener.class);
                mPopupWindow.setOnItemSelectedListener(mOnItemSelectedListener);
                mPopupWindow.setListSelector(mActivity.getDrawable(R.drawable.red_fill));
            }

            if (mHasDismissListener) {
                mOnDismissListener = mock(PopupWindow.OnDismissListener.class);
                mPopupWindow.setOnDismissListener(mOnDismissListener);
            }

            mPopupWindow.setModal(mIsModal);
            if (mHasWindowLayoutType) {
                mPopupWindow.setWindowLayoutType(mWindowLayoutType);
            }

            if (!mIgnoreContentWidth) {
                mPopupWindow.setContentWidth(
                        getContentWidth(listPopupAdapter, mPopupWindow.getBackground()));
            }

            if (mHorizontalOffset != 0) {
                mPopupWindow.setHorizontalOffset(mHorizontalOffset);
            }

            if (mVerticalOffset != 0) {
                mPopupWindow.setVerticalOffset(mVerticalOffset);
            }

            if (mDropDownGravity != Gravity.NO_GRAVITY) {
                mPopupWindow.setDropDownGravity(mDropDownGravity);
            }

            if (mPromptView != null) {
                mPopupWindow.setPromptPosition(mPromptPosition);
                mPopupWindow.setPromptView(mPromptView);
            }

            mInstrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    mPopupWindow.show();
                    assertTrue(mPopupWindow.isShowing());
                }
            });
            mInstrumentation.waitForIdleSync();
        }

        private void showAgain() {
            mInstrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    if (mPopupWindow == null || mPopupWindow.isShowing()) {
                        return;
                    }
                    mPopupWindow.show();
                    assertTrue(mPopupWindow.isShowing());
                }
            });
            mInstrumentation.waitForIdleSync();
        }

        private void dismiss() {
            mInstrumentation.runOnMainSync(new Runnable() {
                public void run() {
                    if (mPopupWindow == null || !mPopupWindow.isShowing())
                        return;
                    mPopupWindow.dismiss();
                }
            });
            mInstrumentation.waitForIdleSync();
        }
    }
}
