/*
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

package org.apache.guacamole.auth.jdbc.sharing.user;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.sharing.connection.SharedConnectionDirectory;
import org.apache.guacamole.auth.jdbc.sharing.connectiongroup.SharedRootConnectionGroup;
import org.apache.guacamole.auth.jdbc.user.RemoteAuthenticatedUser;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.ConnectionRecordSet;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroupDirectory;
import org.apache.guacamole.net.auth.simple.SimpleConnectionRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;

/**
 * The user context of a SharedUser, providing access ONLY to the user
 * themselves, the any SharedConnections associated with that user via share
 * keys, and an internal root connection group containing only those
 * connections.
 */
public class SharedUserContext implements UserContext {

    /**
     * The AuthenticationProvider that created this SharedUserContext.
     */
    private AuthenticationProvider authProvider;

    /**
     * The user whose level of access is represented by this user context.
     */
    private User self;

    /**
     * A directory of all connections visible to the user for whom this user
     * context was created.
     */
    @Inject
    private SharedConnectionDirectory connectionDirectory;

    /**
     * A directory of all connection groups visible to the user for whom this
     * user context was created.
     */
    private Directory<ConnectionGroup> connectionGroupDirectory;

    /**
     * A directory of all users visible to the user for whom this user context
     * was created.
     */
    private Directory<User> userDirectory;

    /**
     * The root connection group of the hierarchy containing all connections
     * and connection groups visible to the user for whom this user context was
     * created.
     */
    private ConnectionGroup rootGroup;

    /**
     * Creates a new SharedUserContext which provides access ONLY to the given
     * user, the SharedConnections associated with the share keys used by that
     * user, and an internal root connection group containing only those
     * SharedConnections.
     *
     * @param authProvider
     *     The AuthenticationProvider that created this
     *     SharedUserContext;
     *
     * @param user
     *     The RemoteAuthenticatedUser for whom this SharedUserContext is being
     *     created.
     */
    public void init(AuthenticationProvider authProvider, RemoteAuthenticatedUser user) {

        // Associate the originating authentication provider
        this.authProvider = authProvider;

        // Provide access to all connections shared with the given user
        this.connectionDirectory.init(user);

        // The connection group directory contains only the root group
        this.rootGroup = new SharedRootConnectionGroup(this);
        this.connectionGroupDirectory = new SimpleConnectionGroupDirectory(
                Collections.singletonList(this.rootGroup));

        // Create internal pseudo-account representing the authenticated user
        this.self = new SharedUser(user, this);

        // Do not provide access to any user accounts via the directory
        this.userDirectory = new SimpleDirectory<User>();

    }

    /**
     * Registers a new share key with this SharedUserContext such that the user
     * will have access to the connection associated with that share key. The
     * share key will be automatically de-registered when it is no longer valid.
     *
     * @param shareKey
     *     The share key to register.
     */
    public void registerShareKey(String shareKey) {
        connectionDirectory.registerShareKey(shareKey);
    }

    @Override
    public User self() {
        return self;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return null;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Directory<User> getUserDirectory() {
        return userDirectory;
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory() {
        return connectionGroupDirectory;
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<ActiveConnection>();
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<SharingProfile>();
    }

    @Override
    public ConnectionRecordSet getConnectionHistory() {
        return new SimpleConnectionRecordSet();
    }

    @Override
    public ConnectionGroup getRootConnectionGroup() {
        return rootGroup;
    }

    @Override
    public Collection<Form> getUserAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getSharingProfileAttributes() {
        return Collections.<Form>emptyList();
    }

}
