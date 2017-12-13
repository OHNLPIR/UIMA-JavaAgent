UIMA Java Agent
===
This application takes advantage of the Java virtual machine's instrumentation capabilities to perform runtime 
modifications to the compiled bytecode of UIMA pipeline components.

This is done so as to apply patches to UIMA in a form that is easily distributable: patches contributed directly to the 
UIMA project, while certainly planned, are slow to propagate throughout the ecosystem: dependent UIMA applications
must first recompile their source code/update to include the updated UIMA library before these performance improvements
can be observed. This especially becomes a problem in situations where multiple UIMA-based applications are deployed 
throughout an organization. On the other hand, performance enhancements included here are deployable 
simply as a jar file with an added command line parameter when booting up the UIMA pipelie. 

Moreover, this allows for a degree of flexibility: the performance enhancements included here should be compatible with
99% of running UIMA applications and use cases, but in the 1% that it does not, one can simply choose not to run the
agent and program will remain unaffected

## Usage

`java -javaagent:PATH/TO/UIMA-JavaAgent.jar ...` to enable, host program will remain uninstrumented if the -javaagent flag is not set 
