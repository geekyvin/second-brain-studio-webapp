
import { useState } from "react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { 
  PenLine, 
  CheckSquare, 
  Calendar, 
  Star, 
  Lightbulb, 
  PlusCircle,
  Search,
  ChevronDown,
  ChevronRight,
  Pin
} from "lucide-react";

interface SidebarProps {
  minimized: boolean;
  onToggle: () => void;
}

const Sidebar = ({ minimized, onToggle }: SidebarProps) => {
  const [activeItem, setActiveItem] = useState<string>("all-notes");
  
  return (
    <div 
      className={`h-screen bg-white border-r border-gray-200 transition-all duration-300 ease-in-out ${
        minimized ? "w-20" : "w-64"
      } flex flex-col shadow-sm z-10`}
    >
      {/* Logo and brand */}
      <div className="p-4 border-b border-gray-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-brand-orange rounded-lg flex items-center justify-center text-white font-bold">
            SB
          </div>
          {!minimized && <h1 className="font-bold text-gray-800 animate-fade-in">Second Brain</h1>}
        </div>
        
        <Button 
          variant="ghost" 
          size="icon" 
          onClick={onToggle} 
          className="text-gray-400 hover:text-gray-800"
          aria-label={minimized ? "Expand sidebar" : "Collapse sidebar"}
        >
          {minimized ? <ChevronRight size={16} /> : <ChevronDown size={16} />}
        </Button>
      </div>
      
      {/* Search */}
      <div className="p-4">
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
          <Input 
            placeholder={minimized ? "Search" : "Search notes..."} 
            className="pl-9 bg-gray-50 border-gray-200 focus:bg-white"
          />
        </div>
      </div>
      
      {/* Navigation items */}
      <div className="flex-1 overflow-y-auto px-3 py-2">
        <nav className="space-y-0.5 stagger-children">
          <button 
            className={activeItem === "daily-logs" ? "sidebar-item-active" : "sidebar-item"}
            onClick={() => setActiveItem("daily-logs")}
          >
            <Calendar size={18} />
            {!minimized && <span>Daily Logs</span>}
          </button>
          
          <button 
            className={activeItem === "all-notes" ? "sidebar-item-active" : "sidebar-item"}
            onClick={() => setActiveItem("all-notes")}
          >
            <PenLine size={18} />
            {!minimized && <span>All Notes</span>}
          </button>
          
          <button 
            className={activeItem === "tasks" ? "sidebar-item-active" : "sidebar-item"}
            onClick={() => setActiveItem("tasks")}
          >
            <CheckSquare size={18} />
            {!minimized && <span>Tasks</span>}
          </button>
          
          {!minimized && (
            <div className="pt-6 pb-2">
              <div className="flex items-center justify-between px-3 py-2">
                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Pinned Notes</span>
                <Pin size={14} className="text-gray-400" />
              </div>
            </div>
          )}
          
          <button className="sidebar-item">
            <Star size={18} className="text-amber-400" />
            {!minimized && <span>Favorites</span>}
          </button>
          
          <button className="sidebar-item">
            <Lightbulb size={18} className="text-amber-400" />
            {!minimized && <span>Tips</span>}
          </button>
        </nav>
      </div>
      
      {/* Create new */}
      <div className="p-4 border-t border-gray-100">
        <Button 
          className={`w-full justify-start gap-2 ${minimized ? "px-2" : ""}`}
          variant="outline"
        >
          <PlusCircle size={18} />
          {!minimized && <span>New Note</span>}
        </Button>
      </div>
    </div>
  );
};

export default Sidebar;
