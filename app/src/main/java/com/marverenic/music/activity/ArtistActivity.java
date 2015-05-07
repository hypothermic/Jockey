package com.marverenic.music.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.ArtistPageAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Themes;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ArtistActivity extends BaseActivity {

    public static final String ARTIST_EXTRA = "artist";

    @Override
    public void onCreate(Bundle savedInstanceState){
        Object parent = getIntent().getParcelableExtra(ARTIST_EXTRA);
        if (parent instanceof Artist){
            setContentLayout(R.layout.activity_artist);
        }
        else{
            setContentLayout(R.layout.page_error);
        }
        super.onCreate(savedInstanceState);

        if (parent instanceof Artist) {

            if (getSupportActionBar() != null) getSupportActionBar().setTitle(parent.toString());

            ArrayList<Song> songEntries;
            ArrayList<Album> albumEntries;

            songEntries = LibraryScanner.getArtistSongEntries((Artist) parent);
            Library.sortSongList(songEntries);
            albumEntries = LibraryScanner.getArtistAlbumEntries((Artist) parent);
            Library.sortAlbumList(albumEntries);

            ListView list = (ListView) findViewById(R.id.list);
            ArtistPageAdapter adapter = new ArtistPageAdapter(this, songEntries, albumEntries);
            list.setAdapter(adapter);
            list.setOnItemClickListener(adapter);
            initializeArtistHeader(albumEntries, ((Artist) parent).artistName);
        }
    }

    private void initializeArtistHeader(ArrayList<Album> albums, final String artistName) {
        final Context context = this;
        final View bioHeader = View.inflate(this, R.layout.artist_header_bio, null);
        final View imageHeader = View.inflate(this, R.layout.artist_header_image, null);

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Fetch.ArtistBio bio = Fetch.fetchArtistBio(context, artistName);
                if (bio != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(bio.artURL != null && !bio.artURL.equals(""))
                                Picasso.with(context).load(bio.artURL).placeholder(R.drawable.art_default)
                                        .into((ImageView) imageHeader);

                            String bioText;
                            if (!bio.tags[0].equals("")) {
                                bioText = bio.tags[0].toUpperCase().charAt(0) + bio.tags[0].substring(1);
                                if (!bio.summary.equals("")) {
                                    bioText = bioText + " - " + bio.summary;
                                }
                            }
                            else bioText = bio.summary;

                            if (bioText.trim().equals("")) bioText = context.getResources().getString(R.string.no_description);

                            TextView bioTextView = (TextView) bioHeader.findViewById(R.id.artist_bio);

                            bioTextView.setText(bioText);
                            ObjectAnimator bioTextAnimator = ObjectAnimator.ofObject(bioTextView,
                                    "textColor",
                                    new ArgbEvaluator(),
                                    Color.TRANSPARENT,
                                    Themes.getDetailText());
                            bioTextAnimator.setDuration(300).start();
                        }
                    });
                }
                else{
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView bioTextView = (TextView) bioHeader.findViewById(R.id.artist_bio);
                            bioTextView.setText(R.string.no_description);
                            bioTextView.setTextColor(Themes.getDetailText());
                        }
                    });
                }
            }
        }).start();

        final View albumHeader = View.inflate(this, R.layout.artist_header_albums, null);
        final GridView albumGrid = (GridView) albumHeader.findViewById(R.id.albumGrid);
        AlbumGridAdapter gridAdapter = new AlbumGridAdapter(albums, context);
        albumGrid.setAdapter(gridAdapter);

        int albumCount = albums.size();

        final ListView listView = (ListView) findViewById(R.id.list);
        listView.addHeaderView(imageHeader, null, false);
        listView.addHeaderView(bioHeader, null, true);
        listView.addHeaderView(albumGrid, null, false);

        updateArtistGridLayout((GridView) findViewById(R.id.albumGrid), albumCount);
    }

    private void updateArtistGridLayout(GridView albumGrid, int albumCount) {
        final boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp > 700;
        final short screenWidth = (short) getResources().getConfiguration().screenWidthDp;
        final float density = getResources().getDisplayMetrics().density;
        final short globalPadding = (short) (getResources().getDimension(R.dimen.global_padding) / density);
        final short gridPadding = (short) (getResources().getDimension(R.dimen.grid_padding) / density);
        final short extraHeight = (short) (4 * gridPadding
                + (getResources().getDimension(R.dimen.grid_text_header_size) / density)
                + (getResources().getDimension(R.dimen.grid_text_detail_size) / density));
        final short minWidth = (short) (getResources().getDimension(R.dimen.grid_width) / density);

        short availableWidth = (short) (screenWidth - 2 * (((isTablet)? globalPadding : 0) + gridPadding));
        double numColumns = (availableWidth + gridPadding) / (minWidth + gridPadding);

        short columnWidth = (short) Math.floor(availableWidth / numColumns);
        short rowHeight = (short) (columnWidth + extraHeight);

        short numRows = (short) Math.ceil(albumCount / numColumns);

        short gridHeight = (short) (rowHeight * numRows + 2 * gridPadding);

        int height = (int) ((gridHeight * density));

        ViewGroup.LayoutParams albumParams = albumGrid.getLayoutParams();
        albumParams.height = height;
        albumGrid.setLayoutParams(albumParams);
    }

    @Override
    public void themeActivity() {
        super.themeActivity();
        findViewById(R.id.list).setBackgroundColor(Themes.getBackgroundElevated());
    }

    @Override
    public void update() {
        updateMiniplayer();
    }
}
