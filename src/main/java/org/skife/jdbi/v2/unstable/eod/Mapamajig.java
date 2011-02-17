/*
 * Copyright 2004 - 2011 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Mapamajig
{
    private final List<MapperFactory> factories = new CopyOnWriteArrayList<MapperFactory>();

    private final ConcurrentHashMap<ResolvedType, ResultSetMapper> cache = new ConcurrentHashMap<ResolvedType, ResultSetMapper>();

    public void add(ResultSetMapper mapper)
    {
        this.add(new InferredMapperFactory(mapper));
    }

    public void add(MapperFactory factory)
    {
        factories.add(factory);
        cache.clear();
    }

    ResultSetMapper mapperFor(ResolvedMethod method, ResolvedType returnType)
    {
        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            Mapper mapper = method.getRawMember().getAnnotation(Mapper.class);
            try {
                return mapper.value().newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException("unable to invoke default ctor on " + method, e);
            }
        }


        ResultSetMapper cached_mapper = cache.get(returnType);
        if (cached_mapper != null) {
            return cached_mapper;
        }

        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            Mapper mapper = method.getRawMember().getAnnotation(Mapper.class);
            try {
                final ResultSetMapper rsm = mapper.value().newInstance();
                cache.put(returnType, rsm);
                return rsm;
            }
            catch (Exception e) {
                throw new RuntimeException("unable to invoke default ctor on " + method, e);
            }
        }


        for (MapperFactory factory : factories) {
            if (factory.accepts(returnType.getErasedType())) {
                final ResultSetMapper mapper = factory.mapperFor(returnType.getErasedType());
                cache.put(returnType, mapper);
                return mapper;
            }
        }

        throw new UnsupportedOperationException("Not Yet Implemented!");
    }
}
