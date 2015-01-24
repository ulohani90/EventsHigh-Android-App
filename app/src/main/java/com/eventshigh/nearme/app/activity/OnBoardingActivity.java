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
        setContentView(R.layout.activity_on_boarding);

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

        // TODO: fix these resources with the actual on boarding resources
        private final ViewResources[] viewResourceses = new ViewResources[]{
                new ViewResources(R.string.ui_connect_facebook, R.drawable.ic_onboarding_map),
                new ViewResources(R.string.ui_referrer_default, R.drawable.ic_onboarding_city),
                new ViewResources(R.string.register_message, R.drawable.ic_onboarding_follow),
                new ViewResources(R.string.failed_login, R.drawable.ic_onboarding_share),
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
            View view = getLayoutInflater().inflate(R.layout.viewpager_onboard, container, false);
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
