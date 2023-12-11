/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jndi;

import lombok.NonNull;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * Simple implementation of a JNDI naming context. Only supports binding plain Objects to String
 * names. Mainly for test environments.
 *
 * @see SimpleNamingContextBuilder
 * @see org.springframework.jndi.JndiTemplate#createInitialContext
 * @since 1.0
 */
public class SimpleNamingContext implements Context {

    private static final String ROOT_NAME = "";

    private final String contextRoot;

    private final ConcurrentMap<String, Object> bindings = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> environment = new ConcurrentHashMap<>();

    public SimpleNamingContext() {
        this(ROOT_NAME, Map.of());
    }

    SimpleNamingContext(@NonNull String root, @NonNull Map<String, Object> env) {
        this(root, Map.of(), env);
    }

    SimpleNamingContext(
            @NonNull String root,
            @NonNull Map<String, Object> boundObjects,
            @NonNull Map<String, Object> env) {

        this.contextRoot = root;
        this.bindings.putAll(boundObjects);
        this.environment.putAll(env);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String root) throws NamingException {
        return new NameClassPairEnumeration(this, rootName(root));
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String root) throws NamingException {
        return new BindingEnumeration(this, rootName(root));
    }

    private String rootName(@NonNull String root) {
        if (ROOT_NAME.equals(root) || root.endsWith("/")) return root;
        return "%s/".formatted(root);
    }

    @Override
    public Object lookup(@NonNull String lookupName) throws NameNotFoundException {
        final String name = this.contextRoot + lookupName;
        if (name.isEmpty()) {
            return new SimpleNamingContext(this.contextRoot, this.bindings, this.environment);
        }
        Object found = this.bindings.get(name);
        if (found != null) {
            return found;
        }

        final String root = rootName(name);

        if (this.bindings.keySet().stream().anyMatch(boundName -> boundName.startsWith(root))) {
            return new SimpleNamingContext(root, this.bindings, this.environment);
        }

        throw new NameNotFoundException(
                "'%s' not bound. Bindings: %s".formatted(name, this.bindings.keySet()));
    }

    @Override
    public Object lookupLink(String name) throws NameNotFoundException {
        return lookup(name);
    }

    @Override
    public void bind(String name, Object obj) {
        this.bindings.put(this.contextRoot + name, obj);
    }

    @Override
    public void unbind(String name) {
        this.bindings.remove(this.contextRoot + name);
    }

    @Override
    public void rebind(String name, Object obj) {
        bind(name, obj);
    }

    @Override
    public void rename(String oldName, String newName) throws NameNotFoundException {
        Object obj = lookup(oldName);
        unbind(oldName);
        bind(newName, obj);
    }

    @Override
    public Context createSubcontext(String name) {
        final String subcontextName = rootName(this.contextRoot + name);
        Context subcontext =
                new SimpleNamingContext(subcontextName, this.bindings, this.environment);
        bind(name, subcontext);
        return subcontext;
    }

    @Override
    public void destroySubcontext(String name) {
        unbind(name);
    }

    @Override
    public String composeName(String name, String prefix) {
        return prefix + name;
    }

    @Override
    public Hashtable<String, Object> getEnvironment() {
        return new Hashtable<>(this.environment);
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) {
        return this.environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) {
        return this.environment.remove(propName);
    }

    @Override
    public void close() {
        this.environment.clear();
        this.bindings.clear();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public void unbind(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public NameParser getNameParser(String name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        throw nameUnsupported();
    }

    protected OperationNotSupportedException nameUnsupported() {
        return new OperationNotSupportedException("javax.naming.Name is not supported");
    }

    private abstract static class BaseNamingEnumeration<T> implements NamingEnumeration<T> {

        private final Iterator<T> iterator;

        private BaseNamingEnumeration(SimpleNamingContext context, final String root)
                throws NamingException {

            if (!root.equals(ROOT_NAME) && !root.endsWith("/")) {
                throw new IllegalArgumentException("root must end with /: " + root);
            }
            final String contextRoot = context.contextRoot + root;

            Map<String, T> contents = new HashMap<>();
            for (String boundName : context.bindings.keySet()) {
                if (boundName.startsWith(contextRoot)) {
                    final String strippedName = extractSimpleName(contextRoot, boundName);
                    contents.computeIfAbsent(strippedName, name -> lookup(root, name, context));
                }
            }
            if (contents.isEmpty()) {
                throw new NamingException("Invalid root '%s%s'".formatted(contextRoot, root));
            }
            this.iterator = contents.values().iterator();
        }

        protected String extractSimpleName(final String contextRoot, String boundName) {
            int startIndex = contextRoot.length();
            int endIndex = boundName.indexOf('/', startIndex);
            return (endIndex != -1
                    ? boundName.substring(startIndex, endIndex)
                    : boundName.substring(startIndex));
        }

        private T lookup(String root, String name, SimpleNamingContext context) {
            Object lookup;
            try {
                lookup = context.lookup(root + name);
            } catch (NameNotFoundException shouldNotHappen) {
                throw new IllegalStateException(
                        "Subcontext lookup should not fail at this point", shouldNotHappen);
            }
            return createObject(name, lookup);
        }

        protected abstract T createObject(String simpleName, Object obj);

        @Override
        public boolean hasMore() {
            return this.iterator.hasNext();
        }

        @Override
        public T next() {
            return this.iterator.next();
        }

        @Override
        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return this.iterator.next();
        }

        @Override
        public void close() {}
    }

    private static final class NameClassPairEnumeration
            extends BaseNamingEnumeration<NameClassPair> {

        private NameClassPairEnumeration(SimpleNamingContext context, String root)
                throws NamingException {
            super(context, root);
        }

        protected @Override NameClassPair createObject(String simpleName, Object obj) {
            return new org.geoserver.cloud.jndi.SimpleNameClassPair(
                    simpleName, obj.getClass().getName());
        }
    }

    private static final class BindingEnumeration extends BaseNamingEnumeration<Binding> {

        private BindingEnumeration(SimpleNamingContext context, String root)
                throws NamingException {
            super(context, root);
        }

        protected @Override Binding createObject(String simpleName, Object obj) {
            return new Binding(simpleName, obj);
        }
    }
}
