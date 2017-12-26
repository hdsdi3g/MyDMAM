/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 28 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;

interface NodeIOEvent {
	
	void onCloseButChannelWasClosed(ClosedChannelException e);
	
	void onCloseException(IOException e);
	
	void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format);
	
	// void onReadPendingException(ReadPendingException e);
	
	void onRemoveOldStoredDataFrame(long session_id);
	
	void onManualCloseAfterSend();
	
	void onIOButClosed(AsynchronousCloseException e);
	
	void onIOExceptionCauseClosing(Throwable e);
	
	void onManualCloseAfterRecevied();
	
	void onAfterSend(Integer size);
	
	void onAfterReceviedDatas(Integer size);
	
}
