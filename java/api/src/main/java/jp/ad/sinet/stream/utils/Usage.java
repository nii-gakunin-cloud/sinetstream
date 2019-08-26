/*
 * Copyright (C) 2019 National Institute of Informatics
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jp.ad.sinet.stream.utils;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Usage {
    public static void main(String[] args) {
        Usage usage = new Usage();
        usage.usage(System.out);
    }

    private void usage(PrintStream out) {
        StringBuilder sb = new StringBuilder();
        writerUsage(sb);
        readerUsage(sb);
        out.print(sb.toString());
    }

    private void writerUsage(StringBuilder sb) {
        sb.append("==================================================").append('\n');
        writerExample(sb);
        writerFactorySignature(sb);
    }

    private Boolean isNotCommonMethod(Method method) {
        HashSet<String> names = new HashSet<>(Arrays.asList(
                "toString", "wait", "equals", "hashCode", "getClass", "notify", "notifyAll", "build"));
        return !names.contains(method.getName());
    }

    private void writerFactorySignature(StringBuilder sb) {
        appendFactorySignature(sb,
                MessageWriterFactory.builder().build(),
                MessageWriterFactory.MessageWriterFactoryBuilder.class.getMethods());
    }

    private void readerFactorySignature(StringBuilder sb) {
        appendFactorySignature(sb,
                MessageReaderFactory.builder().build(),
                MessageReaderFactory.MessageReaderFactoryBuilder.class.getMethods());
    }

    private Optional<Field> toField(Object target, Method method) {
        String name = method.getName();
        try {
            return Optional.of(target.getClass().getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            try {
                return Optional.of(target.getClass().getDeclaredField(name + 's'));
            } catch (NoSuchFieldException ex) {
                return Optional.empty();
            }
        }
    }

    private void appendFactorySignature(StringBuilder sb, Object target, Method[] methods) {
        Map<String, Object> defaultValues = getDefaultValues(target);
        sb.append(target.getClass().getSimpleName()).append(" parameters:").append('\n');
        Arrays.stream(methods)
                .filter(this::isNotCommonMethod)
                .filter(x -> x.getParameterCount() > 0)
                .sorted((o1, o2) -> fieldComparator(o1, o2, target))
                .forEach(method -> {
                    Optional<Field> field = toField(target, method);
                    if (!field.map(f -> f.getDeclaredAnnotation(Parameter.class)).map(Parameter::hide).orElse(false)) {
                        appendSignatureText(sb, defaultValues, method, field);
                    }
                });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void appendSignatureText(StringBuilder sb, Map<String, Object> defaultValues, Method method, Optional<Field> field) {
        sb.append("    ");
        sb.append(method.getName());
        sb.append('(');
        appendMethodParmeters(sb, defaultValues, method, field);
        sb.append(')');
        appendDescription(sb, field, !field.map(x -> x.getName().equals(method.getName())).orElse(true));
        sb.append('\n');
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void appendMethodParmeters(StringBuilder sb, Map<String, Object> defaultValues, Method method, Optional<Field> field) {
        switch (method.getParameterCount()) {
            case 1:
                sb.append(method.getParameterTypes()[0].getSimpleName()).append(' ').append(method.getName());
                appendDefaultValue(sb, defaultValues, field);
                break;
            case 2:
                sb.append(method.getParameterTypes()[0].getSimpleName()).append(' ').append("key").append(", ")
                        .append(method.getParameterTypes()[1].getSimpleName()).append(' ').append(method.getName());
                break;
            default:
                sb.append(Arrays.stream(method.getParameters())
                        .map(x -> x.getType().getSimpleName() + " " + x.getName())
                        .collect(Collectors.joining(", ")));
                break;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void appendDefaultValue(StringBuilder sb, Map<String, Object> defaultValues, Optional<Field> field) {
        field.map(f -> new SimpleEntry<>(f, f.getDeclaredAnnotation(Parameter.class)))
                .map(tp -> Optional.ofNullable(tp.getValue())
                        .map(Parameter::value).filter(x -> !x.isEmpty()).orElseGet(() -> tp.getKey().getName())
                )
                .map(defaultValues::get)
                .filter(x -> !(x instanceof Duration))
                .map(Object::toString)
                .ifPresent(x -> sb.append("[=").append(x).append("]"));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void appendDescription(StringBuilder sb, Optional<Field> field, Boolean singular) {
        field.map(f -> f.getDeclaredAnnotation(Description.class))
                .map(x -> singular ? x.singular() : x.value())
                .ifPresent(desc -> sb.append('\n').append("        ").append(desc));

        field.map(f -> f.getDeclaredAnnotation(Parameter.class)).filter(Parameter::required)
                .ifPresent(x -> sb.append(" (REQUIRED)"));
    }

    private int fieldComparator(Method o1, Method o2, Object target) {
        final Collator collator = Collator.getInstance();
        Function<Optional<Field>, Boolean> isRequired =
                x -> x.map(f -> f.getDeclaredAnnotation(Parameter.class)).map(Parameter::required).orElse(false);
        Optional<Field> f1 = toField(target, o1);
        Optional<Field> f2 = toField(target, o2);
        boolean req1 = isRequired.apply(f1);
        boolean req2 = isRequired.apply(f2);
        if (req1 == req2) {
            return collator.compare(o1.getName(), o2.getName());
        } else {
            return req1 ? -1 : 1;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDefaultValues(Object target) {
        return Arrays.stream(target.getClass().getDeclaredFields())
                .filter(f -> Objects.nonNull(f.getDeclaredAnnotation(DefaultParameters.class)))
                .findAny()
                .map(f -> {
                    f.setAccessible(true);
                    try {
                        return (Map<String, Object>) f.get(target);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }).orElseGet(Collections::emptyMap);
    }

    private void readerUsage(StringBuilder sb) {
        sb.append("==================================================").append('\n');
        readerExample(sb);
        readerFactorySignature(sb);
    }

    private void writerExample(StringBuilder sb) {
        sb.append("MessageWriter example").append('\n')
                .append("--------------------------------------------------").append('\n')
                .append("MessageWriterFactory<String> factory = MessageWriterFactory.<String>builder()").append('\n')
                .append("        .service(\"service-1\")").append('\n')
                .append("        .topic(\"topic-1\")").append('\n')
                .append("        .build();").append('\n')
                .append("try (MessageWriter<String> writer = factory.getWriter()) {").append('\n')
                .append("    writer.writer(\"message\");").append('\n')
                .append("}").append('\n')
                .append("--------------------------------------------------").append('\n');
    }


    private void readerExample(StringBuilder sb) {
        sb.append("MessageReader example").append('\n')
                .append("--------------------------------------------------").append('\n')
                .append("MessageReaderFactory<String> factory = MessageReaderFactory.<String>builder()").append('\n')
                .append("        .service(\"service-1\")").append('\n')
                .append("        .topic(\"topic-1\")").append('\n')
                .append("        .build();").append('\n')
                .append("try (MessageReader<String> reader = factory.getReader()) {").append('\n')
                .append("    Message<String> msg;").append('\n')
                .append("    while (Objects.nonNull(msg = reader.read)) {").append('\n')
                .append("        System.out.println(msg.getValue());").append('\n')
                .append("    }").append('\n')
                .append("}").append('\n')
                .append("--------------------------------------------------").append('\n');
    }
}