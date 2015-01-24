package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

public class OnBoardingActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {
    private LinearLayout dotsView;
    private ViewPager viewPager;
    private int lastPosition = 0;
    private LinearLayout.LayoutParams bigDotLayoutParams;
    private LinearLayout.LayoutParams smallDotLayoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        int dp4 = Utils.dpToPx(this, 4);
        int dp8 = Utils.dpToPx(this, 8);
        int dp12 = Utils.dpToPx(this, 12);
        smallDotLayoutParams = new LinearLayout.LayoutParams(dp8, dp8);
        smallDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);
        bigDotLayoutParams = new LinearLayout.LayoutParams(dp12, dp12);
        bigDotLayoutParams.setMargins(dp4, dp4, dp4, dp4);

        dotsView = (LinearLayout) findViewById(R.id.dots_parent);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new Adapter());
        viewPager.setOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        dotsView.getChildAt(lastPosition).setLayoutParams(smallDotLayoutParams);
        dotsView.getChildAt(position).setLayoutParams(bigDotLayoutParams);
        lastPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private class Adapter extends PagerAdapter {
        private class ViewResources {
            private final int captionResId;
            private final int imageResId;

            private ViewResources(int captionResId, int imageResId) {
                this.captionResId = captionResId;
                this.imageResId = imageResId;
            }
        }

        private final ViewResources[] viewResourceses = new ViewResources[]{
                new ViewResources(R.string.onboarding_map, R.drawable.ic_onboarding_map),
                new ViewResources(R.string.onboarding_city, R.drawable.ic_onboarding_city),
                new ViewResources(R.string.onboarding_follow, R.drawable.ic_onboarding_follow),
                new ViewResources(R.string.onboarding_share, R.drawable.ic_onboarding_share),
        };

        private final LayoutInflater layoutInflater;

        public Adapter() {
            layoutInflater = getLayoutInflater();
            for (int i = 0; i < viewResourceses.length; i++) {
                View view = layoutInflater.inflate(R.layout.viewpager_dot, dotsView, false);
                dotsView.addView(view);
                view.setLayoutParams(smallDotLayoutParams);
            }
            dotsView.getChildAt(0).setLayoutParams(bigDotLayoutParams);
        }

        @Override
        public int getCount() {
            return viewResourceses.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = layoutInflater.inflate(R.layout.viewpager_onboarding, container, false);
            container.addView(view);
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setImageResource(viewResourceses[position].imageResId);
            TextView caption = (TextView) view.findViewById(R.id.caption);
            caption.setText(viewResourceses[position].captionResId);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }
}
