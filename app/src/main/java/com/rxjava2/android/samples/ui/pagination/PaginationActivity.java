package com.rxjava2.android.samples.ui.pagination;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.rxjava2.android.samples.R;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;

/**
 * Created by amitshekhar on 15/03/17.
 */

public class PaginationActivity extends AppCompatActivity {

    public static final String TAG = PaginationActivity.class.getSimpleName();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private PublishProcessor<Integer> paginator = PublishProcessor.create();
    private PaginationAdapter paginationAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private boolean loading = false;
    private int pageNumber = 1;
    private int visibleThreshold = 3;
    private int lastVisibleItem, totalItemCount;
    private LinearLayoutManager layoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagination);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        paginationAdapter = new PaginationAdapter();
        recyclerView.setAdapter(paginationAdapter);
        setUpLoadMoreListener();
        subscribeForData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    private void setUpLoadMoreListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView,
                                   int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                totalItemCount = layoutManager.getItemCount();
                lastVisibleItem = layoutManager
                        .findLastVisibleItemPosition();
                if (!loading
                        && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    pageNumber++;
                    paginator.onNext(pageNumber);
                    loading = true;
                }
            }
        });
    }

    private void subscribeForData() {

        Disposable disposable = paginator
                .onBackpressureDrop()
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        loading = true;
                        progressBar.setVisibility(View.VISIBLE);
                    }
                })
                .concatMap(new Function<Integer, Publisher<List<String>>>() {
                    @Override
                    public Publisher<List<String>> apply(@NonNull Integer page) throws Exception {
                        return dataFromNetwork(page);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<String>, Boolean>() {
                    @Override
                    public Boolean apply(@NonNull List<String> items) throws Exception {
                        paginationAdapter.addItems(items);
                        paginationAdapter.notifyDataSetChanged();
                        return true;
                    }
                })
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean value) throws Exception {
                        loading = false;
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }).subscribe();

        compositeDisposable.add(disposable);

        paginator.onNext(pageNumber);

    }

    /**
     * Simulation of network data
     */
    private Flowable<List<String>> dataFromNetwork(final int page) {
        return Flowable.just(true)
                .observeOn(AndroidSchedulers.mainThread())
                .delay(2, TimeUnit.SECONDS)
                .map(new Function<Boolean, List<String>>() {
                    @Override
                    public List<String> apply(@NonNull Boolean value) throws Exception {
                        List<String> items = new ArrayList<>();
                        for (int i = 1; i <= 10; i++) {
                            items.add("Item " + (page * 10 + i));
                        }
                        return items;
                    }
                });
    }
}
