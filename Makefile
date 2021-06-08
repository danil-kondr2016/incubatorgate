all: incubator_gate

incubator_gate: incubator_gate.c
	gcc incubator_gate.c -o incubator_gate

clean:
	rm incubator_gate


