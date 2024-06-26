package com.codemaster.git_repo_analyzer.scraper;

import org.w3c.dom.Element;

@FunctionalInterface
interface NodeProcessor {

  void process(Element element);

}
