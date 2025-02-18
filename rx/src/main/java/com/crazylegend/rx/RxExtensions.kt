package com.crazylegend.rx

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.processors.FlowableProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.Subject
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit


/**
 * Created by hristijan on 3/5/19 to long live and prosper !
 */


fun <T : Any> Observable<T>?.safe(factory: (() -> T)? = null) = this
        ?: factory?.let { Observable.just(it()) }
        ?: Observable.empty()

fun <T : Any> Maybe<T>?.safe(factory: (() -> T)? = null) = this
        ?: factory?.let { Maybe.just(it()) }
        ?: Maybe.empty()


fun <T : Any> Flowable<T>?.safe(factory: (() -> T)? = null) = this
        ?: factory?.let { Flowable.just(it()) }
        ?: Flowable.empty()

fun <T : Any> Completable.toSingle(item: () -> T) = andThen(Single.just(item()))

fun <T : Any> FlowableProcessor<T>.canPublish(): Boolean = !hasComplete() && !hasThrowable()

fun <T : Any> Observable<T>.joinToString(
        separator: String? = null,
        prefix: String? = null,
        postfix: String? = null
): Single<String> = collect(
        { StringBuilder(prefix ?: "") })
{ builder: StringBuilder, next: T ->
    builder.append(if (builder.length == prefix?.length ?: 0) "" else separator ?: "").append(next)
}
        .map { it.append(postfix ?: "").toString() }


fun <T : Any> Observable<T>.withIndex(): Observable<IndexedValue<T>> =
        zipWith(Observable.range(0, Int.MAX_VALUE), BiFunction { value, index -> IndexedValue(index, value) })


val mainThreadScheduler = AndroidSchedulers.mainThread()
val newThreadScheduler = Schedulers.newThread()
val ioThreadScheduler = Schedulers.io()
val computationScheduler = Schedulers.computation()
val trampolineScheduler = Schedulers.trampoline()


/**
 * observe on main thread
 * subscribe on new thread
 * unsubsidised on error and on complete and removes the need to handle it afterwards
 * @usage
 * someObservable
 *  .runSafeOnMain()
 *  .subscribe({}, {])
 */
fun <T : Any> Observable<T>.runSafeOnMain(subscribeOn: Scheduler = newThreadScheduler): Observable<T> =
        observeOn(mainThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Observable<T>.runSafeOnIO(subscribeOn: Scheduler = newThreadScheduler): Observable<T> =
        observeOn(ioThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Flowable<T>.runSafeOnMain(subscribeOn: Scheduler = newThreadScheduler): Flowable<T> =
        observeOn(mainThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Flowable<T>.runSafeOnIO(subscribeOn: Scheduler = newThreadScheduler): Flowable<T> =
        observeOn(ioThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Single<T>.runSafeOnMain(subscribeOn: Scheduler = newThreadScheduler): Single<T> =
        observeOn(mainThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnSuccess { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Single<T>.runSafeOnIO(subscribeOn: Scheduler = newThreadScheduler): Single<T> =
        observeOn(ioThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnSuccess { unsubscribeOn(newThreadScheduler) }

fun Completable.runSafeOnMain(subscribeOn: Scheduler = newThreadScheduler): Completable =
        observeOn(mainThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun Completable.runSafeOnIO(subscribeOn: Scheduler = newThreadScheduler): Completable =
        observeOn(ioThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnComplete { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Maybe<T>.runSafeOnMain(subscribeOn: Scheduler = newThreadScheduler): Maybe<T> =
        observeOn(mainThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnSuccess { unsubscribeOn(newThreadScheduler) }

fun <T : Any> Maybe<T>.runSafeOnIO(subscribeOn: Scheduler = newThreadScheduler): Maybe<T> =
        observeOn(ioThreadScheduler)
                .subscribeOn(subscribeOn)
                .doOnError { unsubscribeOn(newThreadScheduler) }
                .doOnSuccess { unsubscribeOn(newThreadScheduler) }


fun Disposable?.unsubscribe() {
    this?.let {
        if (!isDisposed) {
            dispose()
        }
    }
}

fun <T : Any> Observable<T>.asFlowable(backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST)
        : Flowable<T> {
    return this.toFlowable(backpressureStrategy)
}

fun <T : Any> Flowable<T>.asLiveData(): LiveData<T> {
    return LiveDataReactiveStreams.fromPublisher(this)
}

fun <T : Any> Observable<T>.asLiveData(backpressureStrategy: BackpressureStrategy = BackpressureStrategy.LATEST)
        : LiveData<T> {

    return LiveDataReactiveStreams.fromPublisher(this.toFlowable(backpressureStrategy))

}

fun <T, R> Flowable<List<T>>.mapToList(mapper: (T) -> R): Flowable<List<R>> {
    return this.map { it.map { mapper(it) } }
}

fun <T, R> Observable<List<T>>.mapToList(mapper: (T) -> R): Observable<List<R>> {
    return this.map { it.map { mapper(it) } }
}

fun <T : Any, R : Any> Single<List<T>>.mapToList(mapper: ((T) -> R)): Single<List<R>> {
    return flatMap { Flowable.fromIterable(it).map(mapper).toList() }
}


fun <T : Any> Observable<T>.defer(): Observable<T> {
    return Observable.defer { this }
}

fun <T : Any> Single<T>.defer(): Single<T> {
    return Single.defer { this }
}

fun rxTimer(
        oldTimer: Disposable?,
        time: Long,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        thread: Scheduler = Schedulers.computation(),
        observerThread: Scheduler = mainThreadScheduler, action: ((Long) -> Unit)
): Disposable? {
    oldTimer?.dispose()
    return Observable
            .timer(time, unit, thread)
            .observeOn(observerThread)
            .subscribe {
                action.invoke(it)
            }
}

val Disposable?.isNullOrDisposed get() = this == null || isDisposed

val <T> Subject<T>.canPublish get() = !hasComplete() && !hasThrowable()


// region Single
/**
 * Calls subscribe on `this`, with an empty function for both onSuccess and onError.
 */
@Suppress("CheckResult")
fun <T : Any> Single<T>.subscribeIgnoringResult() {
    subscribe({}, {})
}

/**
 * Chains a flapMap to `this` if [predicate] is true, applying [mapper] to the item emitted by the source Single.
 *
 * @param predicate whether or not the mapper function will be applied
 * @param mapper function to transform the emitter item
 *
 * @return the new Single if [predicate] is true, the original Single otherwise
 */
fun <T : Any> Single<T>.flatMapIf(predicate: Boolean, mapper: (T) -> Single<T>): Single<T> =
        if (predicate) flatMap { mapper(it) } else this
// endregion

// region Completable
/**
 * Calls subscribe on `this`, with an empty function for both onComplete and onError.
 */
@Suppress("CheckResult")
fun Completable.subscribeIgnoringResult() {
    subscribe({}, {})
}

/**
 * Creates a Single that will subscribe to `this` and emit after `this` completes.
 *
 * @param item value to be emitted by onSuccess once source Completable calls onComplete
 * @param alternateError optional throwable to be emitted by onError once source Completable call onError. If null then
 * original error is emitted.
 *
 * @return the new Single
 */
fun <T : Any> Completable.emitOnComplete(item: T, alternateError: Throwable? = null): Single<T> =
        Single.create { emitter ->
            subscribe(
                    { emitter.ifNotDisposed { onSuccess(item) } },
                    { error -> emitter.ifNotDisposed { onError(alternateError ?: error) } }
            )
        }

/**
 * Creates a Single that will subscribe to `this` and always call onError after `this` completes, regardless of
 * onComplete or onError.
 *
 * @param error throwable to be emitted by onError once source Completable calls onComplete or onError
 *
 * @return the new Completable
 */
fun <T : Any> Completable.emitErrorOnComplete(error: Throwable): Single<T> =
        Single.create { emitter ->
            subscribe(
                    { emitter.ifNotDisposed { onError(error) } },
                    { emitter.ifNotDisposed { onError(error) } }
            )
        }

/**
 * Creates a Single that will subscribe to `this` and always call onSuccess after `this` completes, regardless of
 * onComplete or onError.
 *
 * @param item value to be emitted by onSuccess once source Completable calls onComplete or onError
 *
 * @return the new Completable
 */
fun <T : Any> Completable.emitFinally(item: T): Single<T> =
        Single.create { emitter ->
            subscribe(
                    { emitter.ifNotDisposed { onSuccess(item) } },
                    { emitter.ifNotDisposed { onSuccess(item) } }
            )
        }

/**
 * Creates a Completable that will subscribe to `this` and once onComplete is called then subscribe will be called on
 * [chainableCompletableInvocation]. The new completable will emit the values emitted by [chainableCompletableInvocation].
 *
 * @param chainableCompletableInvocation Completable source that will only be subscribed to if `this` completes without
 * error
 *
 * @return the new Completable
 */
fun Completable.ifCompletes(chainableCompletableInvocation: () -> Completable): Completable =
        Completable.create { emitter ->
            subscribe(
                    {
                        chainableCompletableInvocation().subscribe(
                                { emitter.ifNotDisposed { onComplete() } },
                                { error -> emitter.ifNotDisposed { onError(error) } }
                        )
                    },
                    { error -> emitter.ifNotDisposed { onError(error) } }
            )
        }
// endregion

// region Observable
/**
 * Calls subscribe on `this`, with an empty function for onNext and onError
 */
@Suppress("CheckResult")
fun <T : Any> Observable<T>.subscribeIgnoringResult() {
    subscribe({}, {})
}

/**
 * Function to peek emissions and possibly filter them out based on a given [predicate].
 *
 * @param predicate filter function to decide whether a given emission will actually be emitted
 */
inline fun <T : Any> Observable<T>.filterNotifications(crossinline predicate: (Notification<T>) -> Boolean): Observable<T> =
        materialize().filter { predicate(it) }.dematerialize { it }
// endregion

// region Emitters
/**
 * Checks if `this` is already disposed before invoking [body].
 *
 * @param body that will only be invoked if isDisposed is false
 */
inline fun <T : Any> MaybeEmitter<T>.ifNotDisposed(body: MaybeEmitter<T>.() -> Unit) {
    if (!isDisposed) body()
}

/**
 * Checks if `this` is already disposed before invoking [body].
 *
 * @param body that will only be invoked if isDisposed is false
 */
inline fun <T : Any> SingleEmitter<T>.ifNotDisposed(body: SingleEmitter<T>.() -> Unit) {
    if (!isDisposed) body()
}

/**
 * Checks if `this` is already disposed before invoking [body].
 *
 * @param body that will only be invoked if isDisposed is false
 */
inline fun CompletableEmitter.ifNotDisposed(body: CompletableEmitter.() -> Unit) {
    if (!isDisposed) body()
}

/**
 * Checks if `this` is already disposed before invoking [body].
 *
 * @param body that will only be invoked if isDisposed is false
 */
inline fun <T : Any> ObservableEmitter<T>.ifNotDisposed(body: ObservableEmitter<T>.() -> Unit) {
    if (!isDisposed) body()
}


/**
 *  In this scenario, you must also manage the Rx publisher subscription across the life of your fragment or activity.
 *  Note that LiveData does NOT handle errors and it expects that errors are treated as states in the data that's held.
 *  In case of an error being emitted by the publisher, an error will be propagated to the main thread and the app will crash.
 * @receiver Flowable<T>
 * @return LiveData<T>
 */
inline fun <reified T : Any> Flowable<T>.asReactivePublisher(): LiveData<T> {
    return LiveDataReactiveStreams.fromPublisher(this)
}

/**
 * Flowables, most consumers will be able to let the library deal with backpressure using operators and not need to worry about ever manually calling request(long).
 * @receiver LiveData<T>
 *     On subscription to the publisher, the observer will attach to the given LiveData.
 *     Once {@link Subscription#request) is called on the subscription object, an observer will be connected to the data stream.
 *     Calling request(Long.MAX_VALUE) is equivalent to creating an unbounded stream with no backpressure.
 *     If request with a finite count reaches 0, the observer will buffer the latest item and emit it to the subscriber when data is again requested.
 *     Any other items emitted during the time there was no backpressure requested will be dropped.
 * @param lifecycleOwner LifecycleOwner
 * @return Publisher<T>
 */
inline fun <reified T> LiveData<T>.toReactivePublisher(lifecycleOwner: LifecycleOwner): Publisher<T> {
    return LiveDataReactiveStreams.toPublisher(lifecycleOwner, this)
}


fun Completable?.makeDBCall(compositeDisposable: CompositeDisposable, onThrow: (error: Throwable) -> Unit = { _ -> }, onComplete: () -> Unit = {}) {
    this?.subscribeOn(ioThreadScheduler)?.observeOn(mainThreadScheduler)?.subscribe({
        onComplete()
    }, {
        onThrow.invoke(it)
    })?.addTo(compositeDisposable)
}


fun CompositeDisposable.makeDBCallCompletable(onComplete: () -> Unit = {}, onThrow: (error: Throwable) -> Unit = { _ -> }, function: () -> Completable?) {
    function()?.subscribeOn(ioThreadScheduler)
            ?.observeOn(mainThreadScheduler)
            ?.subscribe({
                onComplete()
            }, {
                onThrow.invoke(it)
            })?.addTo(this@makeDBCallCompletable)
}


fun <T : Any> Observable<T>.toFlowableLatest(): Flowable<T> = toFlowable(BackpressureStrategy.LATEST)
fun <T : Any> Observable<T>.toFlowableBuffer(): Flowable<T> = toFlowable(BackpressureStrategy.BUFFER)
fun <T : Any> Observable<T>.toFlowableDrop(): Flowable<T> = toFlowable(BackpressureStrategy.DROP)
fun <T : Any> Observable<T>.toFlowableError(): Flowable<T> = toFlowable(BackpressureStrategy.ERROR)
fun <T : Any> Observable<T>.toFlowableMissing(): Flowable<T> = toFlowable(BackpressureStrategy.MISSING)


fun <T : Any> Maybe<T>.toFlowableLatest() = toObservable().toFlowableLatest()
fun <T : Any> Maybe<T>.toFlowableBuffer() = toObservable().toFlowableBuffer()
fun <T : Any> Maybe<T>.toFlowableDrop() = toObservable().toFlowableDrop()
fun <T : Any> Maybe<T>.toFlowableError() = toObservable().toFlowableError()
fun <T : Any> Maybe<T>.toFlowableMissing() = toObservable().toFlowableMissing()

fun <T : Any> Single<T>.toFlowableLatest() = toObservable().toFlowableLatest()
fun <T : Any> Single<T>.toFlowableBuffer() = toObservable().toFlowableBuffer()
fun <T : Any> Single<T>.toFlowableDrop() = toObservable().toFlowableDrop()
fun <T : Any> Single<T>.toFlowableError() = toObservable().toFlowableError()
fun <T : Any> Single<T>.toFlowableMissing() = toObservable().toFlowableMissing()


fun <T, R : Any> Maybe<T>.mapSelf(mapper: T.() -> R) = map { it.mapper() }

fun <T : Any> Subject<T>.canPublish(): Boolean = !hasComplete() && !hasThrowable()


fun <T : Any> Observable<T>?.subscribeSafely() = this?.subscribe({}, {})

fun <T : Any> Observable<T>?.subscribeSafely(consumer: Consumer<T>) =
        this?.subscribe(consumer, Consumer<Throwable> { })

inline fun <reified T : Any> Observable<T>.applyNetworkSchedulers(): Observable<T> {
    return this.subscribeOn(ioThreadScheduler)
            .observeOn(mainThreadScheduler)
}

inline fun <reified T : Any> Observable<T>.applyComputationSchedulers(): Observable<T> {
    return this.subscribeOn(Schedulers.computation()).observeOn(mainThreadScheduler)
}

inline fun <reified T : Any> Observable<T>.intervalRequest(duration: Long): Observable<T> {
    return this.throttleFirst(duration, TimeUnit.MILLISECONDS)
}

inline fun <reified T : Any> Flowable<T>.applyNetworkSchedulers(): Flowable<T> {
    return this.subscribeOn(ioThreadScheduler)
            .observeOn(mainThreadScheduler)
}

inline fun <reified T : Any> Flowable<T>.applyComputationSchedulers(): Flowable<T> {
    return this.subscribeOn(Schedulers.computation()).observeOn(mainThreadScheduler)
}

inline fun <reified T : Any> Flowable<T>.intervalRequest(duration: Long): Flowable<T> {
    return this.throttleFirst(duration, TimeUnit.MILLISECONDS)
}

inline fun <reified T : Any> Single<T>.applyNetworkSchedulers(): Single<T> {
    return this.subscribeOn(ioThreadScheduler)
            .observeOn(mainThreadScheduler)
}

inline fun <reified T : Any> Single<T>.applyComputationSchedulers(): Single<T> {
    return this.subscribeOn(Schedulers.computation()).observeOn(mainThreadScheduler)
}


inline fun <reified T> Maybe<T>.applyNetworkSchedulers(): Maybe<T> {
    return this.subscribeOn(ioThreadScheduler)
            .observeOn(mainThreadScheduler)
}

inline fun <reified T> Maybe<T>.applyComputationSchedulers(): Maybe<T> {
    return this.subscribeOn(Schedulers.computation()).observeOn(mainThreadScheduler)
}

fun CompositeDisposable.clearAndDispose() {
    clear()
    dispose()
}