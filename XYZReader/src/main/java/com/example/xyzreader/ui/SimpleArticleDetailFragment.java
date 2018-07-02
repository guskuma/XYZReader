package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;


public class SimpleArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "SimpleArticleDetailFrag";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private boolean mBinded = false;
    private String mArticleTitle;
    private String mImageUrl;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);


    public SimpleArticleDetailFragment() {}

    public static SimpleArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        SimpleArticleDetailFragment fragment = new SimpleArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_simple_article_detail, container, false);
        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {

            mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            mImageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));

            getActivity().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText(mCursor.getString(ArticleLoader.Query.BODY))
                            .getIntent(), getString(R.string.action_share)));
                }
            });

            mBinded = true;

        }
    }

    public void changeContainer() {

        if(mBinded) {
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mImageUrl, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(final ImageLoader.ImageContainer imageContainer, boolean b) {
                            final CollapsingToolbarLayout collapsingToolbarLayout = ((CollapsingToolbarLayout)getActivity().findViewById(R.id.toolbar_layout));
                            final ImageView articlePhoto = ((ImageView) getActivity().findViewById(R.id.photo));

                            collapsingToolbarLayout.setTitle(mArticleTitle);
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette p) {

                                        int darkMutedColor = p.getDarkMutedColor(getResources().getColor(R.color.dark_gray));
                                        int lightMutedColor = p.getLightMutedColor(getResources().getColor(R.color.light_gray));
                                        int darkVibrantColor = p.getDarkVibrantColor(getResources().getColor(R.color.dark_gray));
                                        int lightVibrantColor = p.getLightVibrantColor(getResources().getColor(R.color.light_gray));

                                        articlePhoto.setImageBitmap(imageContainer.getBitmap());
                                        articlePhoto.animate().alpha(1f).setDuration(300);

                                        mRootView.findViewById(R.id.meta_bar).setBackgroundColor(darkMutedColor);
                                        collapsingToolbarLayout.setContentScrimColor(darkMutedColor);
                                        collapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
                                        getActivity().findViewById(R.id.top_progress_bar).setVisibility(View.GONE);
                                        getActivity().findViewById(R.id.scrim_white).animate().alpha(0f).setDuration(300).start();
                                        ((FloatingActionButton)getActivity().findViewById(R.id.fab)).setColorFilter(darkVibrantColor);
                                        mRootView.setBackgroundColor(lightMutedColor);

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            getActivity().findViewById(R.id.fab).setBackgroundTintList(ColorStateList.valueOf(lightVibrantColor));
                                        }

                                    }
                                });
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

        getActivity().findViewById(R.id.app_bar).animate().alpha(1f).setDuration(300);
        getActivity().findViewById(R.id.pager).animate().alpha(1f).setDuration(300);
        getActivity().findViewById(R.id.fab).animate().alpha(1f).setDuration(300);
        getActivity().findViewById(R.id.progress_bar).setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }


}
