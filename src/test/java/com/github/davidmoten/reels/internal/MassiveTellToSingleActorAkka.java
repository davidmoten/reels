package com.github.davidmoten.reels.internal;

import java.util.BitSet;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class MassiveTellToSingleActorAkka {

	public static void main(String[] args) throws InterruptedException {
		System.err.println("AKKA Massive Tell started...");
		long start = System.currentTimeMillis();
		run(100000);
		long end = System.currentTimeMillis();
		System.err.println("finished in " + (end - start));
	}

	public static void run(int messagecount) throws InterruptedException {

		final ActorSystem system = ActorSystem.create("akka-massive");
		ActorRef master = system.actorOf(Master.props(messagecount));

		master.tell(StartMessage.Start, ActorRef.noSender());
		system.getWhenTerminated().toCompletableFuture().join();
	}
	
	
	private enum StartMessage {
		Start
	}

	private static class Master extends AbstractActor {


		static public Props props(int limit) {
			return Props.create(Master.class, () -> new Master(limit));
		}

		private final int limit;
		private final BitSet bitset;

		public Master(int limit) {
			this.limit = limit;
			this.bitset = new BitSet(limit);
			bitset.set(0, limit);
		}

		@Override
		public Receive createReceive() {
			return receiveBuilder()
				.match(StartMessage.class, msg -> start())
				.match(Integer.class, this::runnerReplied)
				.build();
		}
		
		private void start() {
			ActorRef runner = context().actorOf(Runner.props());
			for (int i=0; i<limit; i++) {
				runner.tell(i, self());	
			}
		}
		
		private void runnerReplied(int i) {
			bitset.clear(i);
			if (bitset.isEmpty()) {
				context().system().terminate();	
			}
		}
	}
	
	private static class Runner extends AbstractActor {

		static public Props props() {
			return Props.create(Runner.class, Runner::new);
		}
		
		@Override
		public Receive createReceive() {
			return receiveBuilder()
				.match(Integer.class, i -> run(i))
				.build();
		}
		
		private void run(int i) {
			sender().tell(i, self());
		}
	}

}