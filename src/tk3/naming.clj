(ns tk3.naming
  (:require [clojure.string :as str]
            #_[tk3.utils :as ut]))

;; The system is very sensitive to naming changes
;; be carefull and backward compatible here

(def api-group "tk3.io")
(def api-version "v1")
(def api (str api-group "/" api-version))

(def instance-resource-name (str "jupyterinstances." api-group))
(def instance-resource-kind "JupyterInstance")
(def instance-resource-plural "jupyterinstances")

(def colors ["lightseagreen" "dodgerblue" "cornflowerblue" "blue" "gray" "dimgray" "aqua" "floralwhite" "burlywood" "deepskyblue" "deeppink" "cornsilk" "darkviolet" "firebrick" "black" "blanchedalmond" "gainsboro" "greenyellow" "darkturquoise" "beige" "coral" "ghostwhite" "aquamarine" "brown" "indigo" "forestgreen" "lightgoldenrodyellow" "antiquewhite" "chocolate" "cadetblue" "slategrey" "lightslategray" "navy" "indianred" "chartreuse" "mediumpurple" "aliceblue" "mediumspringgreen" "lightskyblue" "maroon" "mediumblue" "bisque" "navajowhite" "green" "lightyellow" "lightcyan" "magenta" "mintcream" "mediumturquoise" "oldlace" "peru" "blueviolet" "azure" "darkseagreen" "lightslategrey" "lightsteelblue" "slateblue" "fuchsia" "palevioletred" "lawngreen" "rosybrown" "mediumorchid" "lemonchiffon" "pink" "red" "lavender" "plum" "goldenrod" "silver" "lime" "linen" "grey" "mediumseagreen" "darkgrey" "salmon" "darkgreen" "midnightblue" "powderblue" "palegoldenrod" "purple" "mediumaquamarine" "sienna" "dimgrey" "lavenderblush" "saddlebrown" "snow" "khaki" "mediumslateblue" "turquoise" "seagreen" "darkslategray" "ivory" "orangered" "royalblue" "mistyrose" "darkgray" "gold" "yellow" "slategray" "darkkhaki" "limegreen" "hotpink" "moccasin" "yellowgreen" "teal" "lightsalmon" "thistle" "darkmagenta" "white" "lightpink" "wheat" "lightgrey" "sandybrown" "darkcyan" "lightblue" "olive" "steelblue" "honeydew" "lightgray" "mediumvioletred" "violet" "papayawhip" "olivedrab" "cyan" "crimson" "lightcoral" "springgreen" "whitesmoke" "darkslategrey" "lightgreen" "darkgoldenrod" "tan" "orange" "darkblue" "darkorchid" "palegreen" "skyblue" "seashell" "darkslateblue" "orchid" "darksalmon" "rebeccapurple" "darkred" "paleturquoise" "peachpuff" "darkorange" "darkolivegreen" "tomato"])

(def data-path "/data")

(defn resource-name [x]
  (get-in x [:metadata :name]))

(defn secret-name [cluster-name]
  (str "tk3-" cluster-name))

(defn service-name [cluster-name]
  (str "tk3-" cluster-name))

(defn instance-name [cluster color]
  (str (cluster-name cluster) "-" color))

(defn data-volume-name [inst-spec]
  (str (resource-name inst-spec) "-data"))

(defn deployment-name [inst-spec]
  (resource-name inst-spec))

(defn pod-name [inst-spec & postfix]
  (str (resource-name inst-spec)
       (when postfix
         (str "-" (str/join "-" postfix)))))

;; TODO refactor utils and fix cyrcular import issue
(defn timestamp-string []
  (str (.getTime (java.util.Date.))))

(defn instance-labels [role color]
  {:role role
   :color color})

(defn master-service-selector [cluster-name]
  {:service (str "tk3-" cluster-name)
   :role "master"})
