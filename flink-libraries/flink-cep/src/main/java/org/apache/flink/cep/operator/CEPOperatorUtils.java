/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.cep.operator;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.ByteSerializer;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.functions.NullByteKeySelector;
import org.apache.flink.cep.EventComparator;
import org.apache.flink.cep.PatternFlatSelectFunction;
import org.apache.flink.cep.PatternFlatTimeoutFunction;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.PatternTimeoutFunction;
import org.apache.flink.cep.nfa.compiler.NFACompiler;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.util.OutputTag;

/**
 * Utility methods for creating {@link PatternStream}.
 */
public class CEPOperatorUtils {

	/**
	 * Creates a data stream containing results of {@link PatternSelectFunction} to fully matching event patterns.
	 *
	 * @param inputStream stream of input events
	 * @param pattern pattern to be search for in the stream
	 * @param selectFunction function to be applied to matching event sequences
	 * @param outTypeInfo output TypeInformation of selectFunction
	 * @param <IN> type of input events
	 * @param <OUT> type of output events
	 * @return Data stream containing fully matched event sequence with applied {@link PatternSelectFunction}
	 */
	public static <IN, OUT> SingleOutputStreamOperator<OUT> createPatternStream(
			final DataStream<IN> inputStream,
			final Pattern<IN, ?> pattern,
			final EventComparator<IN> comparator,
			final PatternSelectFunction<IN, OUT> selectFunction,
			final TypeInformation<OUT> outTypeInfo) {
		return createPatternStream(inputStream, pattern, outTypeInfo, false, comparator, new OperatorBuilder<IN, OUT>() {
			@Override
			public <KEY> OneInputStreamOperator<IN, OUT> build(
				TypeSerializer<IN> inputSerializer,
				boolean isProcessingTime,
				TypeSerializer<KEY> keySerializer,
				NFACompiler.NFAFactory<IN> nfaFactory,
				boolean migratingFromOldKeyedOperator,
				EventComparator<IN> comparator) {
				return new SelectCepOperator<>(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					migratingFromOldKeyedOperator,
					comparator,
					selectFunction
				);
			}

			@Override
			public String getKeyedOperatorName() {
				return "SelectCepOperator";
			}

			@Override
			public String getOperatorName() {
				return "SelectCepOperator";
			}
		});
	}

	/**
	 * Creates a data stream containing results of {@link PatternFlatSelectFunction} to fully matching event patterns.
	 *
	 * @param inputStream stream of input events
	 * @param pattern pattern to be search for in the stream
	 * @param selectFunction function to be applied to matching event sequences
	 * @param outTypeInfo output TypeInformation of selectFunction
	 * @param <IN> type of input events
	 * @param <OUT> type of output events
	 * @return Data stream containing fully matched event sequence with applied {@link PatternFlatSelectFunction}
	 */
	public static <IN, OUT> SingleOutputStreamOperator<OUT> createPatternStream(
			final DataStream<IN> inputStream,
			final Pattern<IN, ?> pattern,
			final EventComparator<IN> comparator,
			final PatternFlatSelectFunction<IN, OUT> selectFunction,
			final TypeInformation<OUT> outTypeInfo) {
		return createPatternStream(inputStream, pattern, outTypeInfo, false, comparator, new OperatorBuilder<IN, OUT>() {
			@Override
			public <KEY> OneInputStreamOperator<IN, OUT> build(
				TypeSerializer<IN> inputSerializer,
				boolean isProcessingTime,
				TypeSerializer<KEY> keySerializer,
				NFACompiler.NFAFactory<IN> nfaFactory,
				boolean migratingFromOldKeyedOperator,
				EventComparator<IN> comparator) {
				return new FlatSelectCepOperator<>(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					migratingFromOldKeyedOperator,
					comparator,
					selectFunction
				);
			}

			@Override
			public String getKeyedOperatorName() {
				return "FlatSelectCepOperator";
			}

			@Override
			public String getOperatorName() {
				return "FlatSelectCepOperator";
			}
		});
	}

	/**
	 * Creates a data stream containing results of {@link PatternFlatSelectFunction} to fully matching event patterns and
	 * also timeouted partially matched with applied {@link PatternFlatTimeoutFunction} as a sideoutput.
	 *
	 * @param inputStream stream of input events
	 * @param pattern pattern to be search for in the stream
	 * @param selectFunction function to be applied to matching event sequences
	 * @param outTypeInfo output TypeInformation of selectFunction
	 * @param outputTag {@link OutputTag} for a side-output with timeouted matches
	 * @param timeoutFunction function to be applied to timeouted event sequences
	 * @param <IN> type of input events
	 * @param <OUT1> type of fully matched events
	 * @param <OUT2> type of timeouted events
	 * @return Data stream containing fully matched event sequence with applied {@link PatternFlatSelectFunction} that
	 * contains timeouted patterns with applied {@link PatternFlatTimeoutFunction} as side-output
	 */
	public static <IN, OUT1, OUT2> SingleOutputStreamOperator<OUT1> createTimeoutPatternStream(
			final DataStream<IN> inputStream,
			final Pattern<IN, ?> pattern,
			final EventComparator<IN> comparator,
			final PatternFlatSelectFunction<IN, OUT1> selectFunction,
			final TypeInformation<OUT1> outTypeInfo,
			final OutputTag<OUT2> outputTag,
			final PatternFlatTimeoutFunction<IN, OUT2> timeoutFunction) {
		return createPatternStream(inputStream, pattern, outTypeInfo, true, comparator, new OperatorBuilder<IN, OUT1>() {
			@Override
			public <KEY> OneInputStreamOperator<IN, OUT1> build(
				TypeSerializer<IN> inputSerializer,
				boolean isProcessingTime,
				TypeSerializer<KEY> keySerializer,
				NFACompiler.NFAFactory<IN> nfaFactory,
				boolean migratingFromOldKeyedOperator,
				EventComparator<IN> comparator) {
				return new FlatSelectTimeoutCepOperator<>(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					migratingFromOldKeyedOperator,
					comparator,
					selectFunction,
					timeoutFunction,
					outputTag
				);
			}

			@Override
			public String getKeyedOperatorName() {
				return "FlatSelectTimeoutCepOperator";
			}

			@Override
			public String getOperatorName() {
				return "FlatSelectTimeoutCepOperator";
			}
		});
	}

	/**
	 * Creates a data stream containing results of {@link PatternSelectFunction} to fully matching event patterns and
	 * also timeouted partially matched with applied {@link PatternTimeoutFunction} as a sideoutput.
	 *
	 * @param inputStream stream of input events
	 * @param pattern pattern to be search for in the stream
	 * @param selectFunction function to be applied to matching event sequences
	 * @param outTypeInfo output TypeInformation of selectFunction
	 * @param outputTag {@link OutputTag} for a side-output with timeouted matches
	 * @param timeoutFunction function to be applied to timeouted event sequences
	 * @param <IN> type of input events
	 * @param <OUT1> type of fully matched events
	 * @param <OUT2> type of timeouted events
	 * @return Data stream containing fully matched event sequence with applied {@link PatternSelectFunction} that
	 * contains timeouted patterns with applied {@link PatternTimeoutFunction} as side-output
	 */
	public static <IN, OUT1, OUT2> SingleOutputStreamOperator<OUT1> createTimeoutPatternStream(
			final DataStream<IN> inputStream,
			final Pattern<IN, ?> pattern,
			final EventComparator<IN> comparator,
			final PatternSelectFunction<IN, OUT1> selectFunction,
			final TypeInformation<OUT1> outTypeInfo,
			final OutputTag<OUT2> outputTag,
			final PatternTimeoutFunction<IN, OUT2> timeoutFunction) {
		return createPatternStream(inputStream, pattern, outTypeInfo, true, comparator, new OperatorBuilder<IN, OUT1>() {
			@Override
			public <KEY> OneInputStreamOperator<IN, OUT1> build(
				TypeSerializer<IN> inputSerializer,
				boolean isProcessingTime,
				TypeSerializer<KEY> keySerializer,
				NFACompiler.NFAFactory<IN> nfaFactory,
				boolean migratingFromOldKeyedOperator,
				EventComparator<IN> comparator) {
				return new SelectTimeoutCepOperator<>(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					migratingFromOldKeyedOperator,
					comparator,
					selectFunction,
					timeoutFunction,
					outputTag
				);
			}

			@Override
			public String getKeyedOperatorName() {
				return "SelectTimeoutCepOperator";
			}

			@Override
			public String getOperatorName() {
				return "SelectTimeoutCepOperator";
			}
		});
	}

	private static <IN, OUT, K> SingleOutputStreamOperator<OUT> createPatternStream(
			final DataStream<IN> inputStream,
			final Pattern<IN, ?> pattern,
			final TypeInformation<OUT> outTypeInfo,
			final boolean timeoutHandling,
			final EventComparator<IN> comparator,
			final OperatorBuilder<IN, OUT> operatorBuilder) {
		final TypeSerializer<IN> inputSerializer = inputStream.getType().createSerializer(inputStream.getExecutionConfig());

		// check whether we use processing time
		final boolean isProcessingTime = inputStream.getExecutionEnvironment().getStreamTimeCharacteristic() == TimeCharacteristic.ProcessingTime;

		// compile our pattern into a NFAFactory to instantiate NFAs later on
		final NFACompiler.NFAFactory<IN> nfaFactory = NFACompiler.compileFactory(pattern, inputSerializer, timeoutHandling);

		final SingleOutputStreamOperator<OUT> patternStream;

		if (inputStream instanceof KeyedStream) {
			KeyedStream<IN, K> keyedStream = (KeyedStream<IN, K>) inputStream;

			TypeSerializer<K> keySerializer = keyedStream.getKeyType().createSerializer(keyedStream.getExecutionConfig());

			patternStream = keyedStream.transform(
				operatorBuilder.getKeyedOperatorName(),
				outTypeInfo,
				operatorBuilder.build(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					true,
					comparator));
		} else {
			KeySelector<IN, Byte> keySelector = new NullByteKeySelector<>();
			TypeSerializer<Byte> keySerializer = ByteSerializer.INSTANCE;

			patternStream = inputStream.keyBy(keySelector).transform(
				operatorBuilder.getOperatorName(),
				outTypeInfo,
				operatorBuilder.build(
					inputSerializer,
					isProcessingTime,
					keySerializer,
					nfaFactory,
					false,
					comparator
				)).forceNonParallel();
		}

		return patternStream;
	}

	private interface OperatorBuilder<IN, OUT> {
		<K> OneInputStreamOperator<IN, OUT> build(
			TypeSerializer<IN> inputSerializer,
			boolean isProcessingTime,
			TypeSerializer<K> keySerializer,
			NFACompiler.NFAFactory<IN> nfaFactory,
			boolean migratingFromOldKeyedOperator,
			EventComparator<IN> comparator);

		String getKeyedOperatorName();

		String getOperatorName();
	}
}
