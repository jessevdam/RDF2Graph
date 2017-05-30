#!/usr/bin/env nodejs
'use strict';

var async = require('async');
var fs = require('fs');
var jsonld = require('./jsonld')();
var program = require('commander');

// use 'request' extension to load JSON-LD
jsonld.use('request');

// final async call to handle errors
function _final(err, results) {
  if(err) {
    if(err instanceof Error) {
      console.log(err.toString());
    }
    else if(typeof err === 'object') {
      console.log('Error:', JSON.stringify(err, null, 2));
    }
    else {
      console.log('Error:', err);
    }
    process.exit(1);
  }
}

program.command('convert [filename]')
  .description('convert quads to JSON-LD')
  .action(function(input, cmd) {
   // process.stdout.write("running" + input + "\n");
    async.auto({
      convert: function(callback, results) {
		fs.readFile(input, 'utf8', function(err,rawdata) {
          jsonld.fromRDF(rawdata, {format: 'application/nquads'}, function(err, data) {
             var output = JSON.stringify(data, null, 2);
             process.stdout.write(output);
          });
        });
      }
    }, _final);
  });
  
program.command('compact [filename]')
  .description('compact JSON-LD')
  .option('-c, --context <filename|URL>', 'context filename or URL')
  .option('-S, --no-strict', 'disable strict mode')
  .option('-A, --no-compact-arrays','disable compacting arrays to single values')
  .option('-g, --graph', 'always output top-level graph [false]')
  .action(function(input, cmd) {
    async.auto({
      readContext: function(callback) {
        // use built-in context loader
        jsonld.request(cmd.context, {}, function(err, res, data) {
          callback(err, data);
        });
      },
      readInput: function(callback) {
        jsonld.request(input, {}, function(err, res, data) {
          callback(err, data);
        });
      },
      process: ['readContext', 'readInput', function(results,callback) {
        var options = {};
        options.strict = cmd.strict;
        options.compactArrays = cmd.compactArrays;
        options.graph = !!cmd.graph;
        jsonld.compact(results.readInput, results.readContext, options,
          function(err, compacted) {
            callback(err, compacted);
          });
      }],
      output: ['process', function(results,callback) {
      	 var output = JSON.stringify(results.process, null, 2);
         process.stdout.write(output);
         process.stdout.write("\n");
        //_output(results.process, cmd, callback);
      }]
    }, _final);
  });  
  
  
program.command('frame [filename]')
  .description('frame JSON-LD')
  .option('-f, --frame <filename|URL>', 'framing filename')
  .action(function(input, cmd) {
    async.auto({
      readFrame: function(callback, results) {
        // use built-in context loader
          jsonld.request(cmd.frame, {}, function(err, res, data) {
            callback(err, data);
        });
      },
      readInput: function(callback, results) {
        jsonld.request(input, {}, function(err, res, data) {
          callback(err, data);
        });
      },
      process: ['readInput', 'readFrame', function(results,callback) {
        var options = {};
        options.embed = true
        options.explicit = false
        options.omitDefault = true
        
        jsonld.frame(results.readInput, results.readFrame, options,
          function(err, framed) {
            callback(err, framed);
          });
      }],
      output: ['process', function(results,callback) {
      	 var output = JSON.stringify(results.process, null, 2);
         process.stdout.write(output);
         process.stdout.write("\n");
        //_output(results.process, cmd, callback);
      }]
    }, _final);
  });   
  
program.command('test [filename]')
  .description('test')
  .action(function(input, cmd) {
    async.auto({
      readInput: function(callback, results) {
        jsonld.request(input, {}, function(err, res, data) {
          callback(err, data);
        });
      },
      process: ['readInput', function(callback, results) {
        jsonld.objectify(results.readInput['@graph'], results.readInput['@context'], {expandContext:results.readInput['@context']},
          function(err, framed) {
            callback(err, framed);
          });
      }],
      process2: ['process', function(callback, results) {
      	     process.stdout.write("======================");
      	     process.stdout.write(JSON.stringify(results.process, null, 2) + "\n");
      	     var object = results.process['@graph'][1];//.source;
         	 for(var key in object) {
               process.stdout.write("" + key + " = " + object[key] + "\n");
             }
      	     /*process.stdout.write(results.process['@graph'][0].source + "\n");
          	 var output = JSON.stringify(results.process['@graph'][0].source, null, 2);
             process.stdout.write(output);*/
             process.stdout.write("\n");
      }]
    }, _final);
  });  

program.parse(process.argv);
