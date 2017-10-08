package com.hgz.test.rxjava2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_SHANGYOU = "上游";
    private static final String TAG_XIAYOU = "下游";
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //创建容器，存放事件
        disposable = new CompositeDisposable();
//        map();
        //新建水管发送给下游，不保证发送的顺序（无序）
//        flatMap();
        //新建水管发送给下游，保证发送的顺序（有序）
//        concatMap();
        //创建两个上游分支，将两个分支发送的事件进行合并
//        zip();
        //上游每发送一个事件就睡眠2秒钟，避免内存的溢出
//        backPressure();
        //每隔两秒，取一个上游数据放入水缸，发送给下游
//        backPressure2();
        //解决背压的新方式
        flowable();
        //只发送long类型数据
//        interval();
    }

    private void interval() {
        Flowable.interval(1, TimeUnit.MILLISECONDS).onBackpressureDrop().observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Long aLong) {
                Log.i(TAG_XIAYOU,aLong+"");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void flowable() {
        Flowable<Integer> upStream=Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                Log.i(TAG_SHANGYOU, "emitter  1"+emitter.requested());
                emitter.onNext(1);
                Log.i(TAG_SHANGYOU, "emitter  2"+emitter.requested());
                emitter.onNext(2);
                Log.i(TAG_SHANGYOU, "emitter  3"+emitter.requested());
                emitter.onNext(3);
                Log.i(TAG_SHANGYOU, "emitter  4"+emitter.requested());
                emitter.onNext(4);
                Log.i(TAG_SHANGYOU, "emitter  complete"+emitter.requested());
                emitter.onComplete();
            }
            //当模式是ERROR的时候  flowable 只能缓存128个事件  不然抛出missBackpressureException
            //BUFFER  容器变大  可以存入更多的事件 不会抛出异常
            //drop 降低的意思  下游只接收当前的事件，抛弃之前发的事件
            //latest 每次都会接收最新的一个事件
        }, BackpressureStrategy.ERROR);
        Subscriber<Integer> subscriber = new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                //切断水管
//                s.cancel();
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG_XIAYOU,integer+"");
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG_XIAYOU,t+"");
            }

            @Override
            public void onComplete() {

            }
        };
        upStream.subscribe(subscriber);
    }

    private void backPressure2() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                for (int i = 0; ; i++) {
//                     Log.i(TAG_SHANGYOU, "emitter  " + i);
                    emitter.onNext(i);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .sample(2,TimeUnit.SECONDS)
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.i(TAG_XIAYOU,integer+"");
                    }
                });
    }

    private void backPressure() {
        Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {

                for (int i = 0; ; i++) {
                    Log.e(TAG_SHANGYOU, "emitter  " + i);
                    e.onNext(i);
                    //没发送一个事件，就睡眠2秒，减缓上游的流速，避免内存溢出
                    Thread.sleep(2000);
                }
            }
        }).subscribeOn(Schedulers.io());

        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Log.e(TAG_SHANGYOU, "emitter  A");
                e.onNext("A");
                Log.e(TAG_SHANGYOU, "emitter  B");
                e.onNext("B");
                Log.e(TAG_SHANGYOU, "emitter  C");
                e.onNext("C");
            }
        }).subscribeOn(Schedulers.io());

        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer + s;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String value) {
                        Log.e(TAG_XIAYOU, value);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void zip() {
        Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.i(TAG_SHANGYOU, "emit 1");
                emitter.onNext(1);
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit 2");
                emitter.onNext(2);
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit 3");
                emitter.onNext(3);
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit 4");
                emitter.onNext(4);
                Thread.sleep(1000);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Log.i(TAG_SHANGYOU, "emit A");
                emitter.onNext("A");
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit B");
                emitter.onNext("B");
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit C");
                emitter.onNext("C");
                Thread.sleep(1000);
                Log.i(TAG_SHANGYOU, "emit D");
                emitter.onNext("D");
                Thread.sleep(1000);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
            @Override
            public String apply(Integer integer, String s) throws Exception {
                return integer+s;
            }
        }).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String value) {
                Log.i(TAG_XIAYOU, value);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void concatMap() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.i(TAG_SHANGYOU,"emitter  1");
                emitter.onNext(1);
                Log.i(TAG_SHANGYOU,"emitter  2");
                emitter.onNext(2);
                Log.i(TAG_SHANGYOU,"emitter  3");
                emitter.onNext(3);
                Log.i(TAG_SHANGYOU,"emitter  4");
                emitter.onNext(4);
                emitter.onComplete();
            }
        }).concatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < 3; i++) {

                    list.add(integer+"何国忠");
                }
                return Observable.fromIterable(list).delay(10,TimeUnit.MILLISECONDS);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i(TAG_XIAYOU,s);
            }
        });
    }

    private void flatMap() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.i(TAG_SHANGYOU,"emitter  1");
                emitter.onNext(1);
                Log.i(TAG_SHANGYOU,"emitter  2");
                emitter.onNext(2);
                Log.i(TAG_SHANGYOU,"emitter  3");
                emitter.onNext(3);
                Log.i(TAG_SHANGYOU,"emitter  4");
                emitter.onNext(4);
                emitter.onComplete();
            }
        }).flatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < 3; i++) {

                    list.add(integer+"何国忠");
                }
                return Observable.fromIterable(list).delay(10, TimeUnit.MILLISECONDS);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Log.i(TAG_XIAYOU,s);
            }
        });
    }

    private void map() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.i(TAG_SHANGYOU,"emitter  1");
                emitter.onNext(1);
                emitter.onComplete();

            }
        }).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer integer) throws Exception {
                return integer+"何国忠";
            }
        }).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                //向容器中添加数据
                disposable.add(d);
            }

            @Override
            public void onNext(String value) {
                Log.i(TAG_XIAYOU,value);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity销毁时，清空容器中的事件
        disposable.clear();
    }
}
