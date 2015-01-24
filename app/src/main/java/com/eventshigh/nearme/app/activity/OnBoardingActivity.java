package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

public class OnBoardingActivity extends FragmentActivity {
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new Adapter());
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
            View view = getLayoutInflater().inflate(R.layout.viewpager_onboarding, container, false);
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
