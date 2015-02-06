#!/usr/bin/env nodejs
'use strict';

var fs = require('fs');
var jsonld = require('/home/jesse/code/ruby/shexvalidate/js/jsonld.js') // require('/home/jesse/programs/jsonld.js/js/jsonld')();
var jade = require('jade');
var _ = require('lodash');

var lib = {};

lib.to_array = function(val)
{
  if(val === undefined)
    return [];
  var res = Array.isArray(val) ? val : [val];
  return res;
};

lib.encodeMultiplicity = function(val)
{
  lib.mulTiplicityComment = null;
  if(val == "http://open-services.net/ns/core#Exactly-one")
    return "";
  else if(val == "http://open-services.net/ns/core#One-or-many")
    return "+";
  else if(val == "http://open-services.net/ns/core#Zero-or-many")
    return "*";
  else if(val == "http://open-services.net/ns/core#Zero-or-one")
    return "?";
  lib.mulTiplicityComment = "No multiplicity defined defaulted to *";
  return "*";
}

lib.encodeType = function(val)
{
  lib.typeComment = null;
  if(val.indexOf("http://www.w3.org/2001/XMLSchema#") == 0)
  {
    return "xsd:" + val.substring("http://www.w3.org/2001/XMLSchema#".length); 
  }
  else if(val == "http://ssb.wur.nl/RDF2Graph/invalid")
  {
    lib.typeComment = "ref to invalid type";
    return ".";
  }
  else if(val == "http://ssb.wur.nl/RDF2Graph/externalref")
  {
    return ".";
  }
  else if(val == "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")
  {
    return "rdf:langString";
  }
  return "@<" + val + ">";
}

lib.comment = function()
{
  if(lib.mulTiplicityComment != null)
  {
    if(lib.typeComment != null)
      return "#" + lib.mulTiplicityComment + " : " + lib.typeComment;
    return "#" + lib.mulTiplicityComment;
  }
  else if(lib.typeComment != null)
    return "#" + lib.typeComment;
}

function objectifyJSONLD(input,callback)
{ 
  jsonld.objectify(input['@graph'], input['@context'], {expandContext:input['@context']},
    function(err, result) {
      callback(err,result);
//    	 console.log(JSON.stringify(result, null, 2));
  });
}

function loadJSON_ld(file,callback) 
{
  fs.readFile(file, 'utf8', function (err, data) {
    if (err) {
      callback(err,null)
      return;
    }

    data = JSON.parse(data);
    jsonld.objectify(data['@graph'], data['@context'], {expandContext:data['@context']}, function(err, result) {
      if(err) 
      {
        callback(err,null)
      }
      else
      {
        callback(null,data['@graph']);
      } 
    });
  });
}

loadJSON_ld(__dirname + "/temp/result.json",  function (err, data) {
  if (err) {
    console.log('Error: ' + err);
    return;
  }
  var fn = jade.compileFile('shex.jade');
  //console.dir(data[0].class);
  var html = fn({"data":  data[0], "_":_ ,"lib":lib} );
  fs.writeFile("temp/out.html", html, function(err) {
    if(err) {
        console.log(err);
    } 
  }); 

});

