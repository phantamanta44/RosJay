# RosJay

RosJay is a ROS client library for Java, designed to run without needing a full ROS install. This might be useful if, for instance, you have a remote machine without ROS that you wish to expose as a ROS node.

## Usage

*TODO: write documentation*

## Examples

An example of the talker/listener nodes from the [roscpp tutorials](http://wiki.ros.org/roscpp_tutorials/Tutorials/WritingPublisherSubscriber) can be found [in the example source directory](https://github.com/phantamanta44/RosJay/tree/master/src/example/java/xyz/phanta/rosjay/example).

## To-do

* Mark all extra threads as daemon threads
* Rename RosData#getDataType to avoid conflicts
* Service providers
* Extensively test ROS datatype serialization code for correctness
* Node remapping
* Implement node bus stats interface
* Better exception semantics
* Quell meaningless exception messages
