(ns phoenix.build.protocols)

(defprotocol BuiltComponent
  (build [_ project]
    "Builds any necessary artifacts, and returns a pair of the updated
    component and the updated project map"))
