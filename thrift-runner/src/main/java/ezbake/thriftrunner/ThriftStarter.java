/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package ezbake.thriftrunner;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Set;
import java.lang.Class;

import ezbake.base.thrift.EzBakeBaseThriftService;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.base.Predicate;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;

import org.apache.commons.lang3.StringUtils;
import java.lang.RuntimeException;

public abstract class ThriftStarter {
    public abstract void initialize() throws IOException;

    public abstract HostAndPort getPublicHostInfo();

    public abstract HostAndPort getPrivateHostInfo();

    public Class<? extends EzBakeBaseThriftService> getServiceClass(URLClassLoader loader) throws Exception {
        return getServiceClassUsingReflectionFromJar(loader);
    }

    protected Class<? extends EzBakeBaseThriftService> getServiceClassUsingReflectionFromJar(URLClassLoader loader)
            throws Exception {
        final Reflections reflections =
                new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader(loader))
                        .addClassLoader(loader));

        final Set<Class<? extends EzBakeBaseThriftService>> types = Sets.filter(reflections.getSubTypesOf(EzBakeBaseThriftService.class), new nonAbstractClassPredicate());

        if(types.size() != 1)
        {
              Function<Class<? extends ezbake.base.thrift.EzBakeBaseThriftService>, String> nameGetter = new Function<Class<? extends ezbake.base.thrift.EzBakeBaseThriftService>, String>()
                  {
                        public String apply(Class<? extends ezbake.base.thrift.EzBakeBaseThriftService> c)
                        {
                              return c.getName();
                        }
                  };

              String errorClasses = Joiner.on(", ").join(Iterables.transform(types, nameGetter));
              throw new RuntimeException("Jar monst contain exactly one EzBakeBaseThriftService. " + types.size() + " classes found: " + errorClasses);
        }

        return types.iterator().next();
    }

    private static class nonAbstractClassPredicate implements Predicate<Class<? extends EzBakeBaseThriftService>> {
          @Override
              public boolean apply(Class<? extends EzBakeBaseThriftService> c) {
                return !Modifier.isAbstract(c.getModifiers());
          }
    }

}
