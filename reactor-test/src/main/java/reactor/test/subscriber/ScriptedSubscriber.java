/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
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

package reactor.test.subscriber;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.test.scheduler.VirtualTimeScheduler;

/**
 * Subscriber implementation that verifies pre-defined expectations as part of its subscription.
 * Typical usage consists of the following steps:
 * <ul>
 * <li>Create a {@code ScriptedSubscriber} builder using {@link #create()} or
 * {@link #create(long)},</li>
 * <li>Set individual up value expectations using
 * {@link SequenceBuilder#expectValue(Object) expectValue(Object)},
 * {@link SequenceBuilder#expectNext(Object[]) expectNext(Object[])},
 * {@link SequenceBuilder#expectNextWith(Predicate) expectNextWith(Predicate)}.</li>
 * and/or
 * <li>Set up subscription actions using either
 * {@link SequenceBuilder#thenRequest(long) thenRequest(long)} or
 * {@link SequenceBuilder#thenCancel() thenCancel()}.
 * </li>
 * <li>Build the {@code ScriptedSubscriber} using
 * {@link TerminationBuilder#expectComplete() expectComplete()},
 * {@link TerminationBuilder#expectError() expectError()},
 * {@link TerminationBuilder#expectError(Class) expectError(Class)},
 * {@link TerminationBuilder#expectErrorWith(Predicate) expectErrorWith(Predicate)}, or
 * {@link TerminationBuilder#thenCancel() thenCancel()}.
 * </li>
 * <li>Subscribe the built {@code ScriptedSubscriber} to a {@code Publisher}.</li>
 * <li>Verify the expectations using either {@link #verify()} or {@link #verify(Duration)}.</li>
 * <li>If any expectations failed, an {@code AssertionError} will be thrown indicating the
 * failures.</li>
 * </ul>
 *
 * <p>For example:
 * <pre>
 * ScriptedSubscriber&lt;String&gt; subscriber = ScriptedSubscriber.&lt;String&gt;create()
 *   .expectValue("foo")
 *   .expectValue("bar")
 *   .expectComplete();
 *
 * Publisher&lt;String&gt; publisher = Flux.just("foo", "bar");
 * publisher.subscribe(subscriber);
 *
 * subscriber.verify();
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Stephane Maldini
 * @since 1.0
 */
public interface ScriptedSubscriber<T> extends Subscriber<T> {

	/**
	 *
	 */
	static void enableVirtualTime(){
		enableVirtualTime(false);
	}

	/**
	 *
	 * @param allSchedulers
	 */
	static void enableVirtualTime(boolean allSchedulers){
		VirtualTimeScheduler.enable(allSchedulers);
	}

	/**
	 *
	 */
	static void disableVirtualTime(){
		VirtualTimeScheduler.reset();
	}

	/**
	 * Verify the signals received by this subscriber. This method will <strong>block</strong>
	 * indefinitely until the stream has been terminated (either through {@link #onComplete()},
	 * {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 * @return the {@link Duration} of the verification
	 * @throws AssertionError in case of expectation failures
	 */
	Duration verify() throws AssertionError;

	/**
	 * Make the specified publisher subscribe to this subscriber and then verify the signals
	 * received by this subscriber. This method will <strong>block</strong>
	 * indefinitely until the stream has been terminated (either through {@link #onComplete()},
	 * {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 * @param publisher the publisher to subscribe to
	 * @return the {@link Duration} of the verification
	 * @throws AssertionError in case of expectation failures
	 */
	Duration verify(Publisher<? extends T> publisher) throws AssertionError;

	/**
	 * Verify the signals received by this subscriber. This method will <strong>block</strong>
	 * for the given duration or until the stream has been terminated (either through
	 * {@link #onComplete()}, {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 * @return the {@link Duration} of the verification
	 * @throws AssertionError in case of expectation failures, or when the verification times out
	 */
	Duration verify(Duration duration) throws AssertionError;

	/**
	 * Make the specified publisher subscribe to this subscriber and then verify the signals
	 * received by this subscriber. This method will <strong>block</strong>
	 * for the given duration or until the stream has been terminated (either through
	 * {@link #onComplete()}, {@link #onError(Throwable)} or {@link Subscription#cancel()}).
	 * @param publisher the publisher to subscribe to
	 * @return the {@link Duration} of the verification
	 * @throws AssertionError in case of expectation failures, or when the verification times out
	 */
	Duration verify(Publisher<? extends T> publisher, Duration duration) throws AssertionError;

	/**
	 * Create a new {@code ScriptedSubscriber} that requests an unbounded amount of values.
	 * @param <T> the type of the subscriber
	 * @return a builder for setting up value expectations
	 */
	static <T> SequenceBuilder<T> create() {
		return create(Long.MAX_VALUE);
	}

	/**
	 * Create a new {@code ScriptedSubscriber} that requests a specified amount of values.
	 * @param n the amount of items to request
	 * @param <T> the type of the subscriber
	 * @return a builder for setting up value expectations
	 */
	static <T> SequenceBuilder<T> create(long n) {
		DefaultScriptedSubscriberBuilder.checkPositive(n);
		return new DefaultScriptedSubscriberBuilder<>(n);
	}

	/**
	 * Define a builder for terminal states.
	 *
	 * @param <T> the type of values that the subscriber contains
	 */
	interface TerminationBuilder<T> {

		/**
		 * Expect an unspecified error.
		 * @return the built subscriber
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectError();

		/**
		 * Expect an error of the specified type.
		 * @param clazz the expected error type
		 * @return the built subscriber
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectError(Class<? extends Throwable> clazz);

		/**
		 * Expect an error with the specified message.
		 * @param errorMessage the expected error message
		 * @return the built subscriber
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectErrorMessage(String errorMessage);

		/**
		 * Expect an error and evaluate with the given predicate.
		 * @param predicate the predicate to test on the next received error
		 * @return the built subscriber
		 * @see Subscriber#onError(Throwable)
		 */
		ScriptedSubscriber<T> expectErrorWith(Predicate<Throwable> predicate);

		/**
		 * Expect an error and consume with the given consumer. Any {@code AssertionError}s
		 * thrown by the consumer will be rethrown during {@linkplain #verify() verification}.
		 * @param consumer the consumer for the exception
		 * @return the built subscriber
		 */
		ScriptedSubscriber<T> consumeErrorWith(Consumer<Throwable> consumer);

		/**
		 * Expect the completion signal.
		 * @return the built subscriber
		 * @see Subscriber#onComplete()
		 */
		ScriptedSubscriber<T> expectComplete();

		/**
		 * Cancel the underlying subscription.
		 * {@link ScriptedSubscriber#create(long)}.
		 * @return the built subscriber
		 * @see Subscription#cancel()
		 */
		ScriptedSubscriber<T> thenCancel();
	}

	/**
	 * Define a builder for expecting individual values.
	 *
	 * @param <T> the type of values that the subscriber contains
	 */
	interface SequenceBuilder<T> extends TerminationBuilder<T> {

		/**
		 *
		 * @return this builder
		 */
		SequenceBuilder<T> advanceTime();

		/**
		 *
		 * @param timeshift
		 * @return this builder
		 */
		SequenceBuilder<T> advanceTimeBy(Duration timeshift);

		/**
		 *
		 * @param instant
		 * @return this builder
		 */
		SequenceBuilder<T> advanceTimeTo(Instant instant);

		/**
		 * Expect an element and consume with the given consumer. Any {@code AssertionError}s
		 * thrown by the consumer will be rethrown during {@linkplain #verify() verification}.
		 * @param consumer the consumer for the value
		 * @return this builder
		 */
		SequenceBuilder<T> consumeNextWith(Consumer<T> consumer);

		/**
		 * Expect the next elements received to be equal to the given values.
		 * @param ts the values to expect
		 * @return this builder
		 * @see Subscriber#onNext(Object)
		 */
		SequenceBuilder<T> expectNext(T... ts);

		/**
		 * Expect an element count starting from the last expectation or onSubscribe.
		 * @param count the predicate to test on the next received value
		 * @return this builder
		 * @see Subscriber#onNext(Object)
		 */
		SequenceBuilder<T> expectNextCount(long count);

		/**
		 * Expect an element and evaluate with the given predicate.
		 * @param predicate the predicate to test on the next received value
		 * @return this builder
		 * @see Subscriber#onNext(Object)
		 */
		SequenceBuilder<T> expectNextWith(Predicate<T> predicate);

		/**
		 * Run an arbitrary task scheduled after previous expectations or tasks.
		 * @param task the task to run
		 * @return this builder
		 */
		SequenceBuilder<T> then(Runnable task);

		/**
		 * Request the given amount of elements from the upstream {@code Publisher}. This is in
		 * addition to the initial number of elements requested by
		 * {@link ScriptedSubscriber#create(long)}.
		 * @param n the number of elements to request
		 * @return this builder
		 * @see Subscription#request(long)
		 */
		SequenceBuilder<T> thenRequest(long n);
	}
}