package com.zhu56.stream;


import com.zhu56.optional.Any;
import com.zhu56.util.ConvertUtil;
import com.zhu56.util.EmptyUtil;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 增强流
 *
 * @author 朱滔
 * @date 2022/11/13 01:11:56
 */
public final class Ztream<T> extends AbstractZtream<T, Ztream<T>> implements
        StreamCollect<T>,
        StreamGroup<T>,
        StreamToMap<T>,
        StreamNumber<T>,
        StreamJoin<T>,
        StreamFilter<T> {

    Ztream(Stream<T> stream) {
        super(stream);
    }

    @Override
    protected Ztream<T> wrap(Stream<T> stream) {
        return new Ztream<>(stream);
    }

    @Override
    public <R> Ztream<R> map(Function<? super T, ? extends R> mapper) {
        return new Ztream<>(stream.map(mapper));
    }

    /**
     * 带索引的元素映射
     *
     * @param mapper 映射器
     * @return {@link Ztream}<{@link R}>
     */
    public <R> Ztream<R> map(BiFunction<? super T, Integer, ? extends R> mapper) {
        return new Ztream<>(stream.map(new IndexedFunction<>(mapper)));
    }

    @Override
    public <R> Ztream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return new Ztream<>(stream.flatMap(mapper));
    }

    /**
     * 返回一个空的串行流
     *
     * @return {@link Ztream}<{@link T}>
     */
    public static <T> Ztream<T> empty() {
        return new Ztream<>(Stream.empty());
    }

    /**
     * 不定量元素创建流
     *
     * @param values 若干元素
     * @return {@link Ztream}<{@link T}>
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Ztream<T> of(T... values) {
        return EmptyUtil.isEmpty(values) ? empty() : of(Stream.of(values));
    }

    /**
     * 由实现{@link Iterable}接口的对象创建非并行流
     *
     * @param iterable iterable
     * @return {@link Ztream}<{@link T}>
     */
    public static <T> Ztream<T> of(Iterable<T> iterable) {
        return of(iterable, false);
    }

    /**
     * 由实现{@link Iterable}接口的对象创建流
     *
     * @param iterable iterable
     * @param parallel 是否并行
     * @return {@link Ztream}<{@link T}>
     */
    public static <T> Ztream<T> of(Iterable<T> iterable, boolean parallel) {
        return Any.of(iterable).map(Iterable::spliterator).map(spliterator -> StreamSupport.stream(spliterator, parallel)).map(Ztream::new).orElseGet(Ztream::empty);
    }

    /**
     * 从标准流创建增强流
     *
     * @param stream 流
     * @return {@link Ztream}<{@link T}>
     */
    public static <T> Ztream<T> of(Stream<T> stream) {
        return Any.of(stream).map(Ztream::new).orElseGet(Ztream::empty);
    }

    /**
     * 按元素某属性升序排序
     *
     * @param fun 属性
     * @return {@link Ztream}<{@link T}>
     */
    public <U extends Comparable<? super U>> Ztream<T> asc(Function<? super T, ? extends U> fun) {
        return sorted(Comparator.comparing(fun));
    }

    /**
     * 按元素某属性降序排序
     *
     * @param fun 有趣
     * @return {@link Ztream}<{@link T}>
     */
    public <U extends Comparable<? super U>> Ztream<T> desc(Function<? super T, ? extends U> fun) {
        return sorted(Comparator.comparing(fun).reversed());
    }

    /**
     * 按某元素去重
     *
     * @param fun 函数
     * @return {@link Ztream}<{@link T}>
     */
    public Ztream<T> distinct(Function<? super T, ?> fun) {
        return distinct(fun, true);
    }

    /**
     * 按某元素去重
     *
     * @param fun      属性
     * @param override 是否向前覆盖
     * @return {@link Ztream}<{@link T}>
     */
    public Ztream<T> distinct(Function<? super T, ?> fun, boolean override) {
        return collect(MyCollectors.distinct(fun, override));
    }

    /**
     * 第一个
     *
     * @return {@link Any}<{@link T}>
     */
    public Any<T> first() {
        return Any.of(findFirst().orElse(null));
    }

    /**
     * 任意一个
     *
     * @return {@link Any}<{@link T}>
     */
    public Any<T> any() {
        return Any.of(findAny().orElse(null));
    }

    /**
     * 如果流集合不为空，则执行以所有元素收集成List为入参的消费函数
     *
     * @param consumer 消费者
     */
    @SuppressWarnings("unchecked")
    public <C extends Collection<T>> void ifNotEmpty(Consumer<C> consumer) {
        C list = (C) this.toList();
        if (EmptyUtil.isNotEmpty(list)) {
            consumer.accept(list);
        }
    }

    /**
     * 遍历
     *
     * @param action 行动
     * @return {@link Ztream}<{@link T}>
     */
    public Ztream<T> peek(ObjIntConsumer<? super T> action) {
        return peek(new IndexedConsumer<>(action));
    }

    /**
     * 遍历
     *
     * @param action 当前元素及遍历下标
     */
    public void forEach(ObjIntConsumer<T> action) {
        forEach(new IndexedConsumer<>(action));
    }

    /**
     * 判断元素是否有重复
     *
     * @return boolean
     */
    public boolean hadRepeat() {
        Set<T> set = new HashSet<>();
        return anyMatch(x -> !set.add(x));
    }

    /**
     * 判断元素某个属性是有重复
     *
     * @param function 函数
     * @return boolean
     */
    public boolean hadRepeat(Function<? super T, ?> function) {
        return map(function).hadRepeat();
    }

    /**
     * 转换
     *
     * @param clazz clazz
     * @return {@link Ztream}<{@link N}>
     */
    public <N> Ztream<N> convert(Class<N> clazz) {
        return map(e -> ConvertUtil.convert(e, clazz));
    }

    /**
     * 追加元素
     *
     * @param values 值
     * @return {@link Ztream}<{@link T}>
     */
    @SafeVarargs
    public final Ztream<T> append(T... values) {
        return this.append(Ztream.of(values).toList());
    }

    /**
     * 追加元素
     *
     * @param iterable iterable
     * @return {@link Ztream}<{@link T}>
     */
    public Ztream<T> append(Iterable<T> iterable) {
        List<T> list = this.toList();
        if (EmptyUtil.isNotEmpty(iterable)) {
            list.addAll(Ztream.of(iterable).toList());
        }
        return Ztream.of(list);
    }

    /**
     * 对元素进行洗牌
     *
     * @return {@link Ztream}<{@link T}>
     */
    public Ztream<T> shuffle() {
        return Ztream.of(Any.of(toList()).peek(Collections::shuffle).orElse(null));
    }

    /**
     * 收集某个集合类型的属性并展开
     * @param mapper
     * @return {@link Ztream}<{@link N}>
     */
    public <N, C extends Collection<N>> Ztream<N> flat(Function<T, C> mapper) {
        return this.map(mapper).flatMap(Ztream::of);
    }

    /**
     * 随机取一个
     *
     * @return {@link Any}<{@link T}>
     */
    public Any<T> randomOne() {
        return shuffle().first();
    }
}
