/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;
import org.teiid.common.buffer.impl.BufferManagerImpl;
import org.teiid.core.types.BlobImpl;
import org.teiid.core.types.BlobType;
import org.teiid.core.types.ClobImpl;
import org.teiid.core.types.ClobType;
import org.teiid.core.types.InputStreamFactory;
import org.teiid.core.types.Streamable;
import org.teiid.core.util.ReaderInputStream;

@SuppressWarnings("nls")
public class TestLobManager {

	@Test
	public void testLobPeristence() throws Exception{
		
		BufferManagerImpl buffMgr = BufferManagerFactory.createBufferManager();
		FileStore fs = buffMgr.createFileStore("temp");
		
		ClobType clob = new ClobType(new ClobImpl(new InputStreamFactory() {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ReaderInputStream(new StringReader("Clob contents One"),  Charset.forName(Streamable.ENCODING)); 
			}
			
		}, -1));
		
		BlobType blob = new BlobType(new BlobImpl(new InputStreamFactory() {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ReaderInputStream(new StringReader("Blob contents Two"),  Charset.forName(Streamable.ENCODING));
			}
			
		}));		
		
		LobManager lobManager = new LobManager();
		lobManager.updateReferences(new int[] {0,1}, Arrays.asList(clob, blob));
		lobManager.persist(fs);
		
		Streamable<?>lob = lobManager.getLobReference(clob.getReferenceStreamId());
		assertTrue(lob.getClass().isAssignableFrom(ClobType.class));
		ClobType clobRead = (ClobType)lob;
		assertEquals(ClobType.getString(clob), ClobType.getString(clobRead));
		assertTrue(clobRead.length() != -1);
		
		lob = lobManager.getLobReference(blob.getReferenceStreamId());
		assertTrue(lob.getClass().isAssignableFrom(BlobType.class));
		BlobType blobRead = (BlobType)lob;
		assertTrue(Arrays.equals(read(blob.getBinaryStream()), read(blobRead.getBinaryStream())));
		
	}
	
	private byte[] read(InputStream in) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bytes = new byte[100];
		while (true) {
			int c = in.read(bytes, 0, 100);
			if (c == -1) {
				break;
			}
			out.write(c);
		}
		return out.toByteArray();
	}
}
