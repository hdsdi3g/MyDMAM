package hd3gtv.mydmam.analysis;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public interface FuturePrepareTask {
	
	void createTask() throws ConnectionException;
	
}
