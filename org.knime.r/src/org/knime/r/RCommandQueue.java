package org.knime.r;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

public class RCommandQueue extends LinkedBlockingQueue<Collection<RCommand>> {
	
    /**
     * Inserts the specified script at the tail of this queue, waiting if
     * necessary for space to become available.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */	
	public void putRScript(final String rScript) throws InterruptedException {
		Collection<RCommand> cmds = new ArrayList<RCommand>();
		StringTokenizer tokenizer = new StringTokenizer(rScript, "\n");
		int c = 1;
		while(tokenizer.hasMoreTokens()) {
			String cmd = tokenizer.nextToken();
			cmds.add(new RCommand(cmd.trim(), c));
			c++;
		}
		put(cmds);
	}

}
