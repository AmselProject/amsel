# Amsel -- The Abstract Malware Symptom Emulation Library

## What is it?
Amsel is a tool for generating symptoms of malware and intruder activity,
in order to test and evaluate threat detectors. 
Symptoms are emulated, that is, whilst they are meant to look realistic,
they should not pose an actual security risk.

Symptom generation is governed by stochastic models such as CTMCs.
If "stochastic models" means nothing to you, think of it as a way of
describing randomness, but in a controlled manner.
Amsel models typically define phases of attacker activity (e.g.
initial infection, discovery, and data exfiltration). Each phase is
associated with some symptoms (e.g. DGA traffic, port scanning,
outgoing data connections). The length of phases, their ordering, the
types and timings of symptoms, and the parameters of symptoms (e.g. which
IPs are accessed) are all parameterised based on stochastic models.

## Whence did it come?
Amsel was developed in Hewlett Packard Labs Bristol, UK as a demonstrator
for malware-modelling techniques. As the Labs were shut down in
January 2017, we decided to release the source code.

## Authors
Amsel was developed by Philipp Reinecke and Stephen Crane.
For any questions, please contact us.

## Why is the code quality so bad, and where is the documentation?
Neither of us is a developer. We wrote this as a demonstrator and as
an experiment. We will try to improve the code and documentation, but
that may take some time. In the meantime, please do not be too upset
by what it looks like, and ask us if you have any questions.

