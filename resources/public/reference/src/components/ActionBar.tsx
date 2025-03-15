
import { Button } from "../components/ui/button";
import { 
  FileText, 
  Mic, 
  Camera, 
  Wand2, 
  FileCheck
} from "lucide-react";

const ActionBar = () => {
  const handleFeatureClick = (feature: string) => {
    console.log(`Selected feature: ${feature}`);
    // Feature logic will be implemented here
  };

  return (
    <div className="border-b border-gray-200 bg-white px-6 py-3 flex items-center justify-between sticky top-0 z-10 shadow-sm">
      <div className="flex items-center space-x-2">
        <h2 className="text-xl font-semibold text-gray-800 mr-6">Untitled</h2>
        
        <div className="flex space-x-2">
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => handleFeatureClick('summarize')}
            className="gap-1.5"
          >
            <FileText size={16} />
            <span>Summarize</span>
          </Button>
          
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => handleFeatureClick('transcribe')}
            className="gap-1.5"
          >
            <Mic size={16} />
            <span>Transcribe</span>
          </Button>
          
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => handleFeatureClick('capture')}
            className="gap-1.5"
          >
            <Camera size={16} />
            <span>Capture</span>
          </Button>
          
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => handleFeatureClick('cocreate')}
            className="gap-1.5"
          >
            <Wand2 size={16} />
            <span>CoCreate</span>
          </Button>
          
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => handleFeatureClick('classify')}
            className="gap-1.5"
          >
            <FileCheck size={16} />
            <span>Classify</span>
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ActionBar;
