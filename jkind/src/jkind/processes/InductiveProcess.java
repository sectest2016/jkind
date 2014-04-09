package jkind.processes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jkind.JKindException;
import jkind.JKindSettings;
import jkind.invariant.Invariant;
import jkind.processes.messages.BaseStepMessage;
import jkind.processes.messages.InductiveCounterexampleMessage;
import jkind.processes.messages.InvalidMessage;
import jkind.processes.messages.InvariantMessage;
import jkind.processes.messages.Message;
import jkind.processes.messages.StopMessage;
import jkind.processes.messages.UnknownMessage;
import jkind.processes.messages.ValidMessage;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.BoolValue;
import jkind.solvers.Model;
import jkind.solvers.NumericValue;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.translation.Keywords;
import jkind.translation.Specification;
import jkind.util.SexpUtil;

public class InductiveProcess extends Process {
	private int kLimit = 0;
	private BaseProcess baseProcess;
	private List<Invariant> invariants = new ArrayList<>();
	private InvariantProcess invariantProcess;
	private ReduceProcess reduceProcess;

	public InductiveProcess(Specification spec, JKindSettings settings, Director director) {
		super("Inductive", spec, settings, director);
	}

	public void setBaseProcess(BaseProcess baseProcess) {
		this.baseProcess = baseProcess;
	}

	public void setInvariantProcess(InvariantProcess invariantProcess) {
		this.invariantProcess = invariantProcess;
	}

	public void setReduceProcess(ReduceProcess reduceProcess) {
		this.reduceProcess = reduceProcess;
	}

	@Override
	public void main() {
		for (int k = 0; k <= settings.n; k++) {
			debug("K = " + k);
			processMessagesAndWait(k);
			assertTransitionAndInvariants(k);
			checkProperties(k);
			if (properties.isEmpty()) {
				break;
			}
		}
		sendStop();
	}

	@Override
	protected void initializeSolver() {
		super.initializeSolver();
		declareN();
	}

	private void processMessagesAndWait(int k) {
		try {
			while (!incoming.isEmpty() || k > kLimit) {
				Message message = incoming.take();
				if (message instanceof InvalidMessage) {
					InvalidMessage invalidMessage = (InvalidMessage) message;
					properties.removeAll(invalidMessage.invalid);
				} else if (message instanceof BaseStepMessage) {
					BaseStepMessage baseStepMessage = (BaseStepMessage) message;
					kLimit = baseStepMessage.step;
				} else if (message instanceof InvariantMessage) {
					InvariantMessage invariantMessage = (InvariantMessage) message;
					invariants.addAll(invariantMessage.invariants);
					assertNewInvariants(invariantMessage.invariants, k - 1);
				} else if (message instanceof UnknownMessage) {
					UnknownMessage unknownMessage = (UnknownMessage) message;
					properties.removeAll(unknownMessage.unknown);
				} else {
					throw new JKindException("Unknown message type in inductive process: "
							+ message.getClass().getCanonicalName());
				}
			}
		} catch (InterruptedException e) {
			throw new JKindException("Interrupted while waiting for message", e);
		}
	}

	private void assertNewInvariants(List<Invariant> invariants, int k) {
		for (int i = 0; i <= k; i++) {
			assertInvariants(invariants, i);
		}
	}

	private void assertInvariants(List<Invariant> invariants, int i) {
		for (Invariant invariant : invariants) {
			assertInvariant(invariant, i);
		}
	}

	private void assertInvariant(Invariant invariant, int i) {
		solver.send(new Cons("assert", invariant.instantiate(getIndex(i))));
	}

	private void assertTransitionAndInvariants(int offset) {
		solver.send(new Cons("assert", new Cons(Keywords.T, getIndex(offset))));
		assertInvariants(invariants, offset);
	}

	private void checkProperties(int k) {
		List<String> possiblyValid = new ArrayList<>(properties);

		while (!possiblyValid.isEmpty()) {
			Result result = solver.query(getInductiveQuery(k, possiblyValid));

			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				BigInteger n = getN(model);
				BigInteger index = n.add(BigInteger.valueOf(k));
				Iterator<String> iterator = possiblyValid.iterator();
				while (iterator.hasNext()) {
					String p = iterator.next();
					BoolValue v = (BoolValue) model.getFunctionValue("$" + p, index);
					if (!v.getBool()) {
						sendInductiveCounterexample(p, n, k + 1, model);
						iterator.remove();
					}
				}
			} else if (result instanceof UnsatResult) {
				properties.removeAll(possiblyValid);
				addPropertiesAsInvariants(k, possiblyValid);
				sendValid(possiblyValid, k);
				return;
			} else if (result instanceof UnknownResult) {
				properties.removeAll(possiblyValid);
				// We report nothing in hopes that the base process at least
				// finds a counterexample
				return;
			}
		}
	}

	private void addPropertiesAsInvariants(int k, List<String> valid) {
		List<Invariant> propertiesAsInvariants = new ArrayList<>();
		for (String property : valid) {
			propertiesAsInvariants.add(new Invariant(property));
		}

		invariants.addAll(propertiesAsInvariants);
		assertNewInvariants(propertiesAsInvariants, k);
	}

	private BigInteger getN(Model model) {
		NumericValue value = (NumericValue) model.getValue(Keywords.N);
		return new BigInteger(value.toString());
	}

	private Sexp getInductiveQuery(int k, List<String> possiblyValid) {
		List<Sexp> hyps = new ArrayList<>();
		for (int i = 0; i < k; i++) {
			hyps.add(SexpUtil.conjoinStreams(possiblyValid, getIndex(i)));
		}
		Sexp conc = SexpUtil.conjoinStreams(possiblyValid, getIndex(k));

		if (hyps.isEmpty()) {
			return conc;
		} else {
			return new Cons("=>", new Cons("and", hyps), conc);
		}
	}

	private Sexp getIndex(int offset) {
		return new Cons("+", Keywords.N, Sexp.fromInt(offset));
	}

	private void sendValid(List<String> valid, int k) {
		baseProcess.incoming.add(new ValidMessage(valid, k, invariants));

		if (reduceProcess != null) {
			reduceProcess.incoming.add(new ValidMessage(valid, k, invariants));
		} else {
			director.incoming.add(new ValidMessage(valid, k, invariants));
		}
	}

	private void sendInductiveCounterexample(String p, BigInteger n, int k, Model model) {
		if (settings.inductiveCounterexamples) {
			director.incoming.add(new InductiveCounterexampleMessage(p, n, k, model));
		}
	}

	private void sendStop() {
		if (invariantProcess != null) {
			invariantProcess.incoming.add(new StopMessage());
		}
		if (reduceProcess != null) {
			reduceProcess.incoming.add(new StopMessage());
		}
	}
}
