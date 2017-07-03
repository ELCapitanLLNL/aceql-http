/*
 * This file is part of AceQL HTTP.
 * AceQL HTTP: SQL Over HTTP                                     
 * Copyright (C) 2017,  KawanSoft SAS
 * (http://www.kawansoft.com). All rights reserved.                                
 *                                                                               
 * AceQL HTTP is free software; you can redistribute it and/or                 
 * modify it under the terms of the GNU Lesser General Public                    
 * License as published by the Free Software Foundation; either                  
 * version 2.1 of the License, or (at your option) any later version.            
 *                                                                               
 * AceQL HTTP is distributed in the hope that it will be useful,               
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU             
 * Lesser General Public License for more details.                               
 *                                                                               
 * You should have received a copy of the GNU Lesser General Public              
 * License along with this library; if not, write to the Free Software           
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  
 * 02110-1301  USA
 * 
 * Any modifications to this file must keep this entire header
 * intact.
 */
package org.kawanfw.sql.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kawanfw.sql.api.server.DatabaseConfigurator;
import org.kawanfw.sql.api.server.session.SessionConfigurator;
import org.kawanfw.sql.servlet.connection.ConnectionStore;
import org.kawanfw.sql.servlet.sql.json_return.JsonErrorReturn;
import org.kawanfw.sql.servlet.sql.json_return.JsonOkReturn;
import org.kawanfw.sql.util.FrameworkDebug;

/**
 * @author Nicolas de Pomereu
 *
 */
public class ServerLogout {

    private static boolean DEBUG = FrameworkDebug.isSet(ServerLogout.class);;
    
    // A space
    public static final String SPACE = " ";
    

    public static void logout(HttpServletRequest request,
	    HttpServletResponse response,
	    DatabaseConfigurator databaseConfigurator) throws IOException {

	PrintWriter out = response.getWriter();

	try {
	    response.setContentType("text/html");

	    String username = request.getParameter(HttpParameter.USERNAME);
	    String sessionId = request.getParameter(HttpParameter.SESSION_ID);

	    SessionConfigurator sessionConfigurator = ServerSqlManager
		    .getSessionManagerConfigurator();
	    sessionConfigurator.remove(sessionId);

	    if (!ConnectionStore.isStateless(username, sessionId)) {
		ConnectionStore connectionStore = new ConnectionStore(username,
			sessionId);
		Connection connection = connectionStore.get();

		if (connection != null) {
		    connectionStore.remove();
		    ConnectionCloser.freeConnection(connection,
			    databaseConfigurator);
		}
	    } else {
		// Nothing to do for stateless connection as no connection
		// created at this point
	    }

	    deleteBlobFiles(databaseConfigurator, username);

	    String jSonReturn = JsonOkReturn.build();

	    if (DEBUG) {
		System.err.println("jSonReturn: " + jSonReturn);
		System.err.println(sessionId);
	    }

	    out.println(jSonReturn);

	} catch (Exception e) {

	    JsonErrorReturn errorReturn = new JsonErrorReturn(response,
		    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		    JsonErrorReturn.ERROR_ACEQL_FAILURE, e.getMessage(),
		    ExceptionUtils.getStackTrace(e));
	    out.println(errorReturn.build());

	}
    }

    /**
     * Delete all files, but do throw error if problem, except development
     * control null pointer exception
     * 
     * @param databaseConfigurator
     * @param username
     */
    private static void deleteBlobFiles(
	    DatabaseConfigurator databaseConfigurator, String username)
	    throws IOException, SQLException {

	if (databaseConfigurator == null) {
	    throw new NullPointerException("databaseConfigurator is null!");
	}

	if (username == null) {
	    throw new NullPointerException("username is null!");
	}

	// Delete all files
	File blobDirectory = databaseConfigurator.getBlobsDirectory(username);
	if (blobDirectory == null || !blobDirectory.exists()) {
	    return;
	}

	File[] files = blobDirectory.listFiles();

	if (files == null) {
	    return;
	}

	for (File file : files) {
	    file.delete();
	}

    }

}