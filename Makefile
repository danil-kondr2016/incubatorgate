all: incubator_gate

incubator_gate: incubator_gate.c
	gcc incubator_gate.c -lcurl -o incubator_gate

clean:
	rm incubator_gate


