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

    private final ConcurrentMap<String, Object> bindings;

    private final Hashtable<String, Object> environment = new Hashtable<>();

    public SimpleNamingContext() {
        this(ROOT_NAME, new Hashtable<>());
    }

    SimpleNamingContext(@NonNull String root, @NonNull Hashtable<String, Object> env) {
        this(root, new ConcurrentHashMap<>(), env);
    }

    SimpleNamingContext(
            @NonNull String root,
            @NonNull ConcurrentMap<String, Object> boundObjects,
            @NonNull Hashtable<String, Object> env) {

        this.contextRoot = root;
        this.bindings = boundObjects;
        if (env != null) {
            this.environment.putAll(env);
        }
    }

    public @Override NamingEnumeration<NameClassPair> list(String root) throws NamingException {
        return new NameClassPairEnumeration(this, rootName(root));
    }

    public @Override NamingEnumeration<Binding> listBindings(String root) throws NamingException {
        return new BindingEnumeration(this, rootName(root));
    }

    private String rootName(@NonNull String root) {
        if (ROOT_NAME.equals(root) || root.endsWith("/")) return root;
        return root + "/";
    }

    public @Override Object lookup(@NonNull String lookupName) throws NameNotFoundException {
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
                "'" + name + "' not bound. Bindings: " + this.bindings.keySet());
    }

    public @Override Object lookupLink(String name) throws NameNotFoundException {
        return lookup(name);
    }

    public @Override void bind(String name, Object obj) {
        this.bindings.put(this.contextRoot + name, obj);
    }

    public @Override void unbind(String name) {
        this.bindings.remove(this.contextRoot + name);
    }

    public @Override void rebind(String name, Object obj) {
        bind(name, obj);
    }

    public @Override void rename(String oldName, String newName) throws NameNotFoundException {
        Object obj = lookup(oldName);
        unbind(oldName);
        bind(newName, obj);
    }

    public @Override Context createSubcontext(String name) {
        final String subcontextName = rootName(this.contextRoot + name);
        Context subcontext =
                new SimpleNamingContext(subcontextName, this.bindings, this.environment);
        bind(name, subcontext);
        return subcontext;
    }

    public @Override void destroySubcontext(String name) {
        unbind(name);
    }

    public @Override String composeName(String name, String prefix) {
        return prefix + name;
    }

    public @Override Hashtable<String, Object> getEnvironment() {
        return this.environment;
    }

    public @Override Object addToEnvironment(String propName, Object propVal) {
        return this.environment.put(propName, propVal);
    }

    public @Override Object removeFromEnvironment(String propName) {
        return this.environment.remove(propName);
    }

    public @Override void close() {}

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override Object lookup(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override Object lookupLink(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override void bind(Name name, Object obj) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override void unbind(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override void rebind(Name name, Object obj) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override void rename(Name oldName, Name newName) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override Context createSubcontext(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override void destroySubcontext(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override String getNameInNamespace() throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override NameParser getNameParser(Name name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override NameParser getNameParser(String name) throws NamingException {
        throw nameUnsupported();
    }

    /**
     * @throws OperationNotSupportedException javax.naming.Name is not supported
     */
    public @Override Name composeName(Name name, Name prefix) throws NamingException {
        throw nameUnsupported();
    }

    protected OperationNotSupportedException nameUnsupported()
            throws OperationNotSupportedException {
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
                    contents.computeIfAbsent(
                            strippedName,
                            name -> {
                                try {
                                    return createObject(name, context.lookup(root + name));
                                } catch (NameNotFoundException e) {
                                    throw new RuntimeException("Should not happen", e);
                                }
                            });
                }
            }
            if (contents.size() == 0) {
                throw new NamingException("Invalid root '" + contextRoot + root + "'");
            }
            this.iterator = contents.values().iterator();
        }

        protected String extractSimpleName(final String contextRoot, String boundName) {
            int startIndex = contextRoot.length();
            int endIndex = boundName.indexOf('/', startIndex);
            String strippedName =
                    (endIndex != -1
                            ? boundName.substring(startIndex, endIndex)
                            : boundName.substring(startIndex));
            return strippedName;
        }

        protected abstract T createObject(String simpleName, Object obj);

        public @Override boolean hasMore() {
            return this.iterator.hasNext();
        }

        public @Override T next() {
            return this.iterator.next();
        }

        public @Override boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

        public @Override T nextElement() {
            return this.iterator.next();
        }

        public @Override void close() {}
    }

    private static final class NameClassPairEnumeration
            extends BaseNamingEnumeration<NameClassPair> {

        private NameClassPairEnumeration(SimpleNamingContext context, String root)
                throws NamingException {
            super(context, root);
        }

        protected @Override NameClassPair createObject(String simpleName, Object obj) {
            return new org.geoserver.cloud.jndi.NameClassPair(simpleName, obj.getClass().getName());
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
